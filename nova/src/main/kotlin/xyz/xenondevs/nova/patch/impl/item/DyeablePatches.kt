package xyz.xenondevs.nova.patch.impl.item

import net.minecraft.core.cauldron.CauldronInteraction
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.item.crafting.ArmorDyeRecipe
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.gets
import xyz.xenondevs.bytebase.util.replaceEvery
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.world.item.behavior.Dyeable

private val CAULDRON_INTERACTION_DYED_ITEM_INTERACTION = ReflectionUtils.getMethodByName(CauldronInteraction::class, "dyedItemIteration")
private val DYED_ARMOR_FOR_EMERALDS = ReflectionUtils.getClass("net.minecraft.world.entity.npc.VillagerTrades\$DyedArmorForEmeralds").kotlin
private val DYED_ARMOR_FOR_EMERALDS_GET_OFFER = ReflectionUtils.getMethodByName(DYED_ARMOR_FOR_EMERALDS, "getOffer")

private val ITEM_STACK_IS_TAG = ReflectionUtils.getMethod(ItemStack::class, "is", TagKey::class)

internal object DyeablePatches : MultiTransformer(CauldronInteraction::class, DYED_ARMOR_FOR_EMERALDS, DyedItemColor::class, ArmorDyeRecipe::class) {
    
    override fun transform() {
        patchItemStackIsItemTagsDyeable(VirtualClassPath[CAULDRON_INTERACTION_DYED_ITEM_INTERACTION])
        patchItemStackIsItemTagsDyeable(VirtualClassPath[DYED_ARMOR_FOR_EMERALDS_GET_OFFER])
        patchItemStackIsItemTagsDyeable(VirtualClassPath[DyedItemColor::applyDyes])
        patchItemStackIsItemTagsDyeable(VirtualClassPath[ArmorDyeRecipe::matches])
        patchItemStackIsItemTagsDyeable(VirtualClassPath[ArmorDyeRecipe::assemble])
    }
    
    private fun patchItemStackIsItemTagsDyeable(node: MethodNode) {
        node.replaceEvery(
            0, 1,
            { invokeStatic(::isDyeable) },
            {
                it.opcode == Opcodes.GETSTATIC && (it as FieldInsnNode).gets(ItemTags::DYEABLE)
                    && it.next.opcode == Opcodes.INVOKEVIRTUAL && (it.next as MethodInsnNode).calls(ITEM_STACK_IS_TAG)
            }
        )
    }
    
    @JvmStatic
    fun isDyeable(itemStack: ItemStack): Boolean {
        val novaItem = itemStack.novaItem
        if (novaItem != null)
            return novaItem.hasBehavior<Dyeable>()
        
        return itemStack.`is`(ItemTags.DYEABLE)
    }
    
}