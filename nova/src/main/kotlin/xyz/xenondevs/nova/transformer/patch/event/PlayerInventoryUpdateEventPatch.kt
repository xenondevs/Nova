package xyz.xenondevs.nova.transformer.patch.event

import net.minecraft.core.NonNullList
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.PlayerDataStorage
import org.bukkit.Bukkit
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.insertBeforeFirst
import xyz.xenondevs.bytebase.util.insertBeforeLast
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.bytebase.util.puts
import xyz.xenondevs.bytebase.util.replaceFirst
import xyz.xenondevs.nova.event.PlayerInventoryItemChangeEvent
import xyz.xenondevs.nova.event.PlayerInventoryUpdateEvent
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.transformer.patch.event.PlayerInventoryUpdateEventPatch.itemStackToPlayerInventory
import xyz.xenondevs.nova.transformer.patch.item.WatchedArmorList
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.collection.ObjectWeakIdentityHashMap
import xyz.xenondevs.nova.util.item.isSimilar
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaMethod
import org.bukkit.entity.Player as BukkitPlayer

/**
 * Replaces the `items` [NonNullList] in [Inventory] with a [WatchedItemList] and patches the [Inventory.load] method to
 * set bot the [WatchedItemList.initialized] and [WatchedArmorList.initialized] fields to true.
 *
 * @see [WatchedArmorList]
 */
internal object PlayerInventoryUpdateEventPatch : MultiTransformer(Inventory::class, PlayerDataStorage::class, ItemStack::class) {
    
    @JvmField
    val itemStackToPlayerInventory = ObjectWeakIdentityHashMap<ItemStack, ItemStackInventoryData>()
    
    override fun transform() {
        patchInventory()
        patchPlayerDataStorage()
        patchItemStackSetCount()
        dumpAll()
    }
    
    /**
     * Replace the [Inventory] `items` list with a [WatchedItemList] and patch the [Inventory.load] method to set the
     * [WatchedItemList.initialized] and [WatchedArmorList.initialized] fields to true.
     */
    private fun patchInventory() {
        VirtualClassPath[ReflectionRegistry.INVENTORY_CONSTRUCTOR].replaceFirst(3, -1, buildInsnList {
            new(WatchedItemList::class)
            dup()
            aLoad(1) // player
            invokeSpecial(WatchedItemList::class.java.constructors[0])
            checkCast(NonNullList::class)
        }) { it.opcode == Opcodes.PUTFIELD && (it as FieldInsnNode).puts(ReflectionRegistry.INVENTORY_ITEMS_FIELD) }
        
        VirtualClassPath[Inventory::load.javaMethod!!].insertBeforeFirst(buildInsnList {
            aLoad(0)
            getField(ReflectionRegistry.INVENTORY_ITEMS_FIELD)
            checkCast(WatchedItemList::class)
            ldc(1)
            putField(WatchedItemList::initialized.javaField!!)
            aLoad(0)
            getField(ReflectionRegistry.INVENTORY_ARMOR_FIELD)
            checkCast(WatchedArmorList::class)
            ldc(1)
            putField(WatchedArmorList::initialized.javaField!!)
            addLabel()
        }) { it.opcode == Opcodes.RETURN }
        
        val addResourceMethod = VirtualClassPath[ReflectionRegistry.INVENTORY_ADD_RESOURCE_METHOD]
        val returnLabel = LabelNode()
        addResourceMethod.localVariables.clear()
        addResourceMethod.instructions.insert(buildInsnList {
            ldc(0)
            iStore(7)
        })
        addResourceMethod.replaceFirst(3, 0, buildInsnList {
            ldc(1)
            iStore(7)
        }) { it is MethodInsnNode && it.calls(Inventory::setItem.javaMethod!!) }
        addResourceMethod.insertBeforeLast(buildInsnList {
            iLoad(7)
            ifeq(returnLabel)
            addLabel()
            aLoad(0)
            iLoad(1)
            aLoad(5)
            invokeVirtual(Inventory::setItem.javaMethod!!)
            add(returnLabel)
        }) { it.next?.opcode == Opcodes.IRETURN }
    }
    
    /**
     * Makes sure that `initialized` is also set to true for new players.
     */
    private fun patchPlayerDataStorage() {
        VirtualClassPath[PlayerDataStorage::load.javaMethod!!].insertBeforeFirst(buildInsnList {
            val returnLabel = LabelNode()
            ifnonnull(returnLabel)
            aLoad(1)
            invokeVirtual(Player::getInventory.javaMethod!!)
            new(ListTag::class.internalName)
            dup()
            invokeSpecial(ReflectionRegistry.LIST_TAG_EMPTY_CONSTRUCTOR)
            invokeVirtual(Inventory::load.javaMethod!!)
            
            add(returnLabel)
            aLoad(2)
        }) { it.opcode == Opcodes.ARETURN }
    }
    
    private fun patchItemStackSetCount() {
        val method = VirtualClassPath[ItemStack::setCount.javaMethod!!]
        method.localVariables.clear()
        method.instructions.insert(buildInsnList {
            addLabel()
            aLoad(0)
            getField(ReflectionRegistry.ITEM_STACK_COUNT_FIELD)
            iLoad(1)
            aLoad(0)
            invokeStatic(InventoryUpdateEventCaller::call.javaMethod!!)
        })
    }
    
}

internal class WatchedItemList(player: Player) : NonNullList<ItemStack>(
    Array<ItemStack>(36) { ItemStack.EMPTY }.asList(),
    ItemStack.EMPTY
) {
    @JvmField
    var initialized = false
    
    private val player = player as? ServerPlayer
    
    override fun set(index: Int, element: ItemStack?): ItemStack {
        if (player != null) {
            val item = element ?: ItemStack.EMPTY
            val oldItem = get(index)
            if (item === oldItem) return oldItem
            
            itemStackToPlayerInventory -= oldItem
            itemStackToPlayerInventory[item] = ItemStackInventoryData(player.bukkitEntity, index)
            
            if (initialized) {
                val previousBukkit = oldItem.takeUnlessEmpty()?.bukkitMirror
                val currentBukkit = item.takeUnlessEmpty()?.bukkitMirror
                
                val updateEvent = PlayerInventoryUpdateEvent(
                    player.bukkitEntity,
                    index,
                    previousBukkit,
                    currentBukkit
                )
                Bukkit.getPluginManager().callEvent(updateEvent)
                if (!oldItem.isSimilar(item)) {
                    val itemChangeEvent = PlayerInventoryItemChangeEvent(
                        player.bukkitEntity,
                        index,
                        previousBukkit,
                        currentBukkit
                    )
                    Bukkit.getPluginManager().callEvent(itemChangeEvent)
                }
            }
        }
        
        return super.set(index, element)
    }
    
    override fun add(element: ItemStack?): Boolean {
        throw UnsupportedOperationException("Cannot add to the items list")
    }
    
}

internal class ItemStackInventoryData(
    @JvmField
    val player: BukkitPlayer,
    @JvmField
    val slot: Int
)