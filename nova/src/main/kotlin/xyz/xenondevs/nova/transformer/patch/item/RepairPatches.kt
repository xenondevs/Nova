package xyz.xenondevs.nova.transformer.patch.item

import net.minecraft.world.inventory.AnvilMenu
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RepairItemRecipe
import net.minecraft.world.level.ItemLike
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.isClass
import xyz.xenondevs.bytebase.util.replaceFirst
import xyz.xenondevs.bytebase.util.replaceFirstRange
import xyz.xenondevs.nova.item.behavior.Damageable
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.util.unwrap

private val ITEM_STACK_ITEM_CONSTRUCTOR = ReflectionUtils.getConstructor(ItemStack::class, ItemLike::class)
private val ITEM_STACK_IS_ITEM_METHOD = ReflectionUtils.getMethod(ItemStack::class, "is", Item::class)

internal object RepairPatches : MultiTransformer(Item::class, AnvilMenu::class, RepairItemRecipe::class) {
    
    override fun transform() {
        patchItemIsValidRepairItem()
        patchRepairItemRecipeAssemble()
        patchAnvilMenuCreateResult()
    }
    
    private fun patchItemIsValidRepairItem() {
        VirtualClassPath[Item::isValidRepairItem].delegateStatic(::isValidRepairItem)
    }
    
    @JvmStatic
    fun isValidRepairItem(item: Item, itemStack: ItemStack, ingredient: ItemStack): Boolean {
        return itemStack.novaItem
            ?.getBehaviorOrNull<Damageable>()
            ?.repairIngredient
            ?.test(ingredient.asBukkitMirror())
            ?: false
    }
    
    /**
     * Intends to replace `itemStack1.is(itemStack2.getItem())` with `isSameItem(itemStack1, itemStack2)`
     */
    private fun patchAnvilMenuCreateResult() {
        VirtualClassPath[AnvilMenu::createResult].replaceFirst(
            0, 1,
            buildInsnList {
                invokeStatic(::isSameItem)
            }
        ) {
            it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(ItemStack::getItem)
                && it.next.opcode == Opcodes.INVOKEVIRTUAL && (it.next as MethodInsnNode).calls(ITEM_STACK_IS_ITEM_METHOD)
        }
    }
    
    @JvmStatic
    fun isSameItem(a: ItemStack, b: ItemStack): Boolean {
        val novaItem = a.novaItem
        if (novaItem != null)
            return novaItem == b.novaItem
        return a.item == b.item
    }
    
    /**
     * Intends to replace `ItemStack itemStack3 = new ItemStack(itemStack.getItem());`
     * with `ItemStack itemStack3 = RepairItemRecipePatch.newItemStack(itemStack);`
     */
    private fun patchRepairItemRecipeAssemble() {
        VirtualClassPath[RepairItemRecipe::assemble].replaceFirstRange(
            { it.opcode == Opcodes.NEW && (it as TypeInsnNode).isClass(ItemStack::class) },
            { it.opcode == Opcodes.INVOKESPECIAL && (it as MethodInsnNode).calls(ITEM_STACK_ITEM_CONSTRUCTOR) },
            -1, 0,
            buildInsnList {
                aLoad(4) // item stack 1
                invokeStatic(::newItemStack)
            }
        )
    }
    
    @JvmStatic
    fun newItemStack(itemStack: ItemStack): ItemStack {
        return itemStack.novaItem?.createItemStack()?.unwrap() ?: ItemStack(itemStack.item)
    }
    
}