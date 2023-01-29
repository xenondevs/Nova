@file:Suppress("unused")

package xyz.xenondevs.nova.transformer.patch.item

import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ItemStack
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.TypeInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.util.isClass
import xyz.xenondevs.bytebase.util.replaceEvery
import xyz.xenondevs.nova.item.behavior.Wearable
import xyz.xenondevs.nova.transformer.MethodTransformer
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.nmsEquipmentSlot
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.LIVING_ENTITY_GET_EQUIPMENT_SLOT_FOR_ITEM_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionUtils

internal object WearablePatch : MethodTransformer(LIVING_ENTITY_GET_EQUIPMENT_SLOT_FOR_ITEM_METHOD, computeFrames = true) {
    
    override fun transform() {
        methodNode.replaceEvery(1, 0, buildInsnList {
            aLoad(0) // ItemStack
            invokeStatic(ReflectionUtils.getMethodByName(WearablePatch::class, false, "isWearableItem"))
        }) { it.opcode == Opcodes.INSTANCEOF && (it as TypeInsnNode).isClass(ArmorItem::class) }
        
        methodNode.replaceEvery(1, 1, buildInsnList {
            aLoad(0) // ItemStack
            invokeStatic(ReflectionUtils.getMethodByName(WearablePatch::class, false, "getEquipmentSlot"))
        }) { it.opcode == Opcodes.CHECKCAST && (it as TypeInsnNode).isClass(ArmorItem::class) }
    }
    
    @JvmStatic
    fun isWearableItem(itemStack: ItemStack): Boolean {
        val novaMaterial = itemStack.novaMaterial
            ?: return itemStack.item is ArmorItem
        
        return novaMaterial.novaItem.hasBehavior(Wearable::class)
    }
    
    @JvmStatic
    fun getEquipmentSlot(itemStack: ItemStack): EquipmentSlot {
        val novaMaterial = itemStack.novaMaterial
            ?: return (itemStack.item as ArmorItem).slot
        
        return novaMaterial.novaItem.getBehavior(Wearable::class)!!.options.armorType.equipmentSlot.nmsEquipmentSlot
    }
    
}