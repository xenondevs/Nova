@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.transformer.patch.item

import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Equipable
import net.minecraft.world.item.ItemStack
import xyz.xenondevs.nova.item.behavior.Wearable
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.nmsEquipmentSlot
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.EQUIPABLE_GET_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionUtils

internal object WearablePatch : MultiTransformer(setOf(Equipable::class), computeFrames = true) {
    
    override fun transform() {
        EQUIPABLE_GET_METHOD.replaceWith(ReflectionUtils.getMethodByName(WearablePatch::class, false, "getEquipable"))
    }
    
    @JvmStatic
    fun getEquipable(itemStack: ItemStack): Equipable? {
        val novaEquipable = getNovaEquipable(itemStack)
        if (novaEquipable != null)
            return novaEquipable
        
        // nms logic
        val item = itemStack.item
        if (item is Equipable)
            return item
        if (item is BlockItem) {
            val block = item.block
            if (block is Equipable)
                return block
        }
        
        return null
    }
    
    fun getNovaEquipable(itemStack: ItemStack): Equipable? {
        val wearable = itemStack.novaMaterial?.novaItem?.getBehavior(Wearable::class)
            ?: return null
        
        return object : Equipable {
            
            override fun getEquipmentSlot() = wearable.options.armorType.equipmentSlot.nmsEquipmentSlot
            
            override fun getEquipSound() = wearable.options.equipSound.let {
                SoundEvent.createVariableRangeEvent(ResourceLocation.tryParse(it))
            } ?: SoundEvents.ARMOR_EQUIP_GENERIC
            
        }
    }
    
}