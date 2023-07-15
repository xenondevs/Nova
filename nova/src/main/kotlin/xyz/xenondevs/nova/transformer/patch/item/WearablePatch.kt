@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.transformer.patch.item

import net.minecraft.core.NonNullList
import net.minecraft.core.dispenser.DispenseItemBehavior
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Equipable
import net.minecraft.world.item.ItemStack
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
import xyz.xenondevs.nova.item.behavior.Wearable
import xyz.xenondevs.nova.player.equipment.ArmorEquipEvent
import xyz.xenondevs.nova.player.equipment.EquipAction
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.bukkitCopy
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.nmsEquipmentSlot
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.DISPENSER_BLOCK_GET_DISPENSE_METHOD_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.INVENTORY_ARMOR_FIELD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.INVENTORY_CONSTRUCTOR

internal object WearablePatch : MultiTransformer(Equipable::class, LivingEntity::class, DispenserBlock::class) {
    
    override fun transform() {
        // makes nova wearables equipable
        Equipable::get.replaceWith(::getEquipable)
        
        // patches the armor inventory to fire the ArmorEquipEvent
        VirtualClassPath[INVENTORY_CONSTRUCTOR].replaceFirst(3, -1, buildInsnList {
            new(WatchedArmorList::class)
            dup()
            aLoad(1) // player
            invokeSpecial(WatchedArmorList::class.java.constructors[0])
            checkCast(NonNullList::class)
        }) { it.opcode == Opcodes.PUTFIELD && (it as FieldInsnNode).puts(INVENTORY_ARMOR_FIELD) }
        
        // patches dispense behavior for nova wearables
        VirtualClassPath[DISPENSER_BLOCK_GET_DISPENSE_METHOD_METHOD].instructions.insert(buildInsnList {
            // val method = getNovaArmorDispenseMethod(itemStack)
            // if (method != null) return method
            // ... default body
            
            val defaultBody = LabelNode()
            addLabel()
            aLoad(1) // itemStack
            invokeStatic(::getNovaArmorDispenseMethod)
            dup()
            ifnull(defaultBody)
            areturn()
            add(defaultBody)
        })
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
    
    fun getNovaEquipable(itemStack: ItemStack): Equipable? {
        val wearable = itemStack.novaItem?.getBehaviorOrNull(Wearable::class)
            ?: return null
        
        return object : Equipable {
            
            override fun getEquipmentSlot() = wearable.slot.nmsEquipmentSlot
            
            override fun getEquipSound() = wearable.equipSound?.let {
                SoundEvent.createVariableRangeEvent(ResourceLocation.tryParse(it))
            } ?: SoundEvents.ARMOR_EQUIP_GENERIC
            
        }
    }
    
    @JvmStatic
    fun getNovaArmorDispenseMethod(itemStack: ItemStack): DispenseItemBehavior? {
        if (itemStack.novaItem?.hasBehavior(Wearable::class) == true)
            return ArmorItem.DISPENSE_ITEM_BEHAVIOR
        
        return null
    }
    
}

internal class WatchedArmorList(player: Player) : NonNullList<ItemStack>(
    Array<ItemStack>(4) { ItemStack.EMPTY }.asList(),
    ItemStack.EMPTY
) {
    
    private val player = player as? ServerPlayer
    private var initialized = false
    private val previousStacks = Array<ItemStack>(4) { ItemStack.EMPTY }
    
    override fun set(index: Int, element: ItemStack?): ItemStack {
        val item = element ?: ItemStack.EMPTY
        if (initialized) {
            if (player != null) {
                val previous = previousStacks[index]
                if (ItemStack.matches(previous, item))
                    return item
                
                val equipAction = when {
                    previous.isEmpty && !item.isEmpty -> EquipAction.EQUIP
                    !previous.isEmpty && item.isEmpty -> EquipAction.UNEQUIP
                    else -> EquipAction.CHANGE
                }
                
                val equipEvent = ArmorEquipEvent(player.bukkitEntity, EquipmentSlot.entries[index + 2], equipAction, previous.bukkitCopy, item.bukkitCopy)
                Bukkit.getPluginManager().callEvent(equipEvent)
                
                if (equipEvent.isCancelled)
                    return item // return the item that was tried to set if the event was cancelled
            }
        } else if (index == 3) {
            // When the player first joins, the players inventory is loaded from nbt, with slot 3 being initialized last
            initialized = true
        }
        
        previousStacks[index] = item.copy()
        return super.set(index, element)
    }
    
    override fun add(element: ItemStack?): Boolean {
        throw UnsupportedOperationException("Cannot add to the armor list")
    }
    
}