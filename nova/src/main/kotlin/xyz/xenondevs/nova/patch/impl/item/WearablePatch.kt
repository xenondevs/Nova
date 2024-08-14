@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.patch.impl.item

import net.minecraft.core.Holder
import net.minecraft.core.NonNullList
import net.minecraft.core.dispenser.DispenseItemBehavior
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Equipable
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.DispenserBlock
import org.bukkit.Bukkit
import org.bukkit.inventory.EquipmentSlot
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.LabelNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.puts
import xyz.xenondevs.bytebase.util.replaceFirst
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.nmsEquipmentSlot
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getField
import xyz.xenondevs.nova.world.item.behavior.Wearable
import xyz.xenondevs.nova.world.player.equipment.ArmorEquipEvent
import xyz.xenondevs.nova.world.player.equipment.EquipAction

private val INVENTORY_CONSTRUCTOR = ReflectionUtils.getConstructor(Inventory::class, Player::class)
private val INVENTORY_ARMOR_FIELD = getField(Inventory::class, true, "armor")
private val DISPENSER_BLOCK_GET_DISPENSE_METHOD = ReflectionUtils.getMethod(DispenserBlock::class, "getDispenseMethod", Level::class, ItemStack::class)
private val SERVER_PLAYER_RESTORE_FROM = ReflectionUtils.getMethod(ServerPlayer::class, "restoreFrom", ServerPlayer::class, Boolean::class)

internal object WearablePatch : MultiTransformer(Equipable::class, LivingEntity::class, DispenserBlock::class, Inventory::class, ServerPlayer::class) {
    
    override fun transform() {
        patchEquipable()
        patchDispenser()
        patchEquipEvent()
    }
    
    private fun patchEquipable() {
        // makes nova wearables equipable
        Equipable::get.replaceWith(::getEquipable)
    }
    
    @JvmStatic
    fun getEquipable(itemStack: ItemStack): Equipable? {
        val novaEquipable = getNovaEquipable(itemStack)
        if (novaEquipable != null)
            return novaEquipable
        
        // -- nms logic --
        val item = itemStack.item
        if (item is Equipable)
            return item
        if (item is BlockItem) {
            val block = item.block
            if (block is Equipable)
                return block
        }
        // ----
        
        return null
    }
    
    // patches dispense behavior for nova wearables
    private fun patchDispenser() {
        VirtualClassPath[DISPENSER_BLOCK_GET_DISPENSE_METHOD].instructions.insert(buildInsnList {
            // val method = getNovaArmorDispenseMethod(itemStack)
            // if (method != null) return method
            // ... default body
            
            val defaultBody = LabelNode()
            addLabel()
            aLoad(2) // itemStack
            invokeStatic(::getNovaArmorDispenseMethod)
            dup()
            ifnull(defaultBody)
            areturn()
            add(defaultBody)
        })
    }
    
    fun getNovaEquipable(itemStack: ItemStack): Equipable? {
        val wearable = itemStack.novaItem?.getBehaviorOrNull(Wearable::class)
            ?: return null
        
        return object : Equipable {
            
            override fun getEquipmentSlot() = wearable.slot.nmsEquipmentSlot
            
            override fun getEquipSound() = wearable.equipSound?.let {
                Holder.direct(SoundEvent.createVariableRangeEvent(ResourceLocation.parse(it)))
            } ?: SoundEvents.ARMOR_EQUIP_GENERIC
            
        }
    }
    
    @JvmStatic
    fun getNovaArmorDispenseMethod(itemStack: ItemStack): DispenseItemBehavior? {
        if (itemStack.novaItem?.hasBehavior(Wearable::class) == true)
            return ArmorItem.DISPENSE_ITEM_BEHAVIOR
        
        return null
    }
    
    // patches the armor inventory to fire the ArmorEquipEvent
    private fun patchEquipEvent() {
        // replaces armor list with WatchedArmorList which fires ArmorEquipEvent
        VirtualClassPath[INVENTORY_CONSTRUCTOR].replaceFirst(3, -1, buildInsnList {
            new(WatchedArmorList::class)
            dup()
            aLoad(1) // player
            invokeSpecial(WatchedArmorList::class.java.constructors[0])
            checkCast(NonNullList::class)
        }) { it.opcode == Opcodes.PUTFIELD && (it as FieldInsnNode).puts(INVENTORY_ARMOR_FIELD) }
        
        // Changing worlds in vanilla creates a new ServerPlayer instance which should also create a new Inventory and WatchedArmorList
        // with the initialized field set to false.
        // However, Spigot reuses ServerPlayer instances, so in some cases ServerPlayer#restoreFrom is called with itself, which causes
        // the armor inventory entries to be set again, firing ArmorEquipEvent on a player that is not alive.
        // See: https://github.com/orgs/PaperMC/projects/6?pane=issue&itemId=16746355 (this should solve this problem)
        // To circumvent this issue, we mark WatchedArmorList as uninitialized again in ServerPlayer#restoreFrom.
        VirtualClassPath[SERVER_PLAYER_RESTORE_FROM].instructions.insert(buildInsnList {
            addLabel()
            aLoad(0)
            invokeVirtual(ServerPlayer::getInventory)
            getField(Inventory::armor)
            checkCast(WatchedArmorList::class)
            ldc(0)
            putField(WatchedArmorList::initialized)
        })
    }
    
}

internal class WatchedArmorList(player: Player) : NonNullList<ItemStack>(
    Array(4) { ItemStack.EMPTY }.asList(),
    ItemStack.EMPTY
) {
    
    @JvmField
    var initialized = false
    private val player = player as? ServerPlayer
    private val previousStacks = Array(4) { ItemStack.EMPTY }
    
    override fun set(index: Int, element: ItemStack): ItemStack {
        if (initialized) {
            if (player !== null) {
                val previous = previousStacks[index]
                if (ItemStack.matches(previous, element))
                    return element
                
                val equipAction = when {
                    previous.isEmpty && !element.isEmpty -> EquipAction.EQUIP
                    !previous.isEmpty && element.isEmpty -> EquipAction.UNEQUIP
                    else -> EquipAction.CHANGE
                }
                
                val equipEvent = ArmorEquipEvent(player.bukkitEntity, EquipmentSlot.entries[index + 2], equipAction, previous.asBukkitCopy(), element.asBukkitCopy())
                Bukkit.getPluginManager().callEvent(equipEvent)
                
                if (equipEvent.isCancelled)
                    return element // return the item that was tried to set if the event was cancelled
            }
        } else if (index == 3) {
            // When the player first joins, the players inventory is loaded from nbt, with slot 3 being initialized last
            initialized = true
        }
        
        previousStacks[index] = element.copy()
        return super.set(index, element)
    }
    
    override fun add(element: ItemStack?): Boolean {
        throw UnsupportedOperationException("Cannot add to the armor list")
    }
    
}