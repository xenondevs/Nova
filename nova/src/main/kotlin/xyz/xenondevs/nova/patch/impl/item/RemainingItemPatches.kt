@file:Suppress("unused")

package xyz.xenondevs.nova.patch.impl.item

import net.minecraft.core.NonNullList
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.BannerDuplicateRecipe
import net.minecraft.world.item.crafting.BookCloningRecipe
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.CraftingRecipe
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.replaceEvery
import xyz.xenondevs.bytebase.util.replaceFirst
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.util.unwrap
import net.minecraft.world.item.ItemStack as MojangStack

private val BREWING_STAND_BLOCK_ENTITY_DO_BREW = ReflectionUtils.getMethodByName(BrewingStandBlockEntity::class, "doBrew")

internal object RemainingItemPatches : MultiTransformer(
    BannerDuplicateRecipe::class, BookCloningRecipe::class, CraftingRecipe::class,
    AbstractFurnaceBlockEntity::class, BrewingStandBlockEntity::class
) {
    
    override fun transform() {
        listOf(
            VirtualClassPath[BannerDuplicateRecipe::getRemainingItems],
            VirtualClassPath[BookCloningRecipe::getRemainingItems],
            VirtualClassPath[BREWING_STAND_BLOCK_ENTITY_DO_BREW]
        ).forEach(::patchGetItemGetCraftingRemainder)
        
        VirtualClassPath[CraftingRecipe::defaultCraftingReminder].delegateStatic(::defaultCraftingRemainder)
        patchAbstractFurnaceBlockEntityServerTick()
    }
    
    /**
     * Replaces ItemStack.getItem().getCraftingRemainder() with RemainingItemStackPatches.getCraftingRemainder(ItemStack)
     *
     * Target instructions:
     * INVOKEVIRTUAL net/minecraft/world/item/ItemStack.getItem ()Lnet/minecraft/world/item/Item;
     * INVOKEVIRTUAL net/minecraft/world/item/Item.getCraftingRemainder ()Lnet/minecraft/world/item/ItemStack;
     */
    private fun patchGetItemGetCraftingRemainder(node: MethodNode) {
        node.localVariables.clear()
        
        node.replaceEvery(
            0, 1,
            { invokeStatic(::getRemainingItemStack) },
            {
                it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(MojangStack::getItem)
                    && it.next.opcode == Opcodes.INVOKEVIRTUAL && (it.next as MethodInsnNode).calls(Item::getCraftingRemainder)
            },
        )
    }
    
    /**
     * Changes
     * ```java
     * Item item = itemStack.getItem();
     * ...
     * furnace.items.set(1, item.getCraftingRemainder());
     * ```
     * to
     * ```java
     * ItemStack remainder = RemainingItemStackPatches.getCraftingRemainder(itemStack);
     * ...
     * furnace.items.set(1, remainder);
     * ```
     */
    private fun patchAbstractFurnaceBlockEntityServerTick() {
        val methodNode = VirtualClassPath[AbstractFurnaceBlockEntity::serverTick]
        methodNode.localVariables.clear()
        
        methodNode.replaceFirst(
            0, 0,
            buildInsnList { invokeStatic(::getRemainingItemStack) }
        ) { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(MojangStack::getItem) }
        methodNode.replaceFirst(
            0, 0,
            buildInsnList { } // no changes, what was previously item is now already the crafting remainder item stack
        ) { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(Item::getCraftingRemainder) }
    }
    
    @JvmStatic
    fun defaultCraftingRemainder(input: CraftingInput): NonNullList<MojangStack> {
        val list = NonNullList.withSize(input.size(), MojangStack.EMPTY);
        
        for (i in list.indices) {
            list[i] = getRemainingItemStack(input.getItem(i));
        }
        return list;
    }
    
    
    @JvmStatic
    fun getRemainingItemStack(itemStack: MojangStack): MojangStack {
        val novaItem = itemStack.novaItem
        if (novaItem != null)
            return novaItem.craftingRemainingItem?.unwrap()?.copy() ?: MojangStack.EMPTY
        
        return itemStack.item.craftingRemainder
    }
    
}