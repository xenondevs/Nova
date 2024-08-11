@file:Suppress("unused")

package xyz.xenondevs.nova.patch.impl.item

import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.BannerDuplicateRecipe
import net.minecraft.world.item.crafting.BookCloningRecipe
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.util.CraftMagicNumbers
import org.bukkit.inventory.ItemStack
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.isClass
import xyz.xenondevs.bytebase.util.replaceEvery
import xyz.xenondevs.bytebase.util.replaceEveryRange
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.ABSTRACT_FURNACE_BLOCK_ENTITY_ITEMS_FIELD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.BREWING_STAND_BLOCK_ENTITY_DO_BREW_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.ITEM_STACK_CONSTRUCTOR
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.NON_NULL_LIST_SET_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.RECIPE_GET_REMAINING_ITEMS_METHOD
import xyz.xenondevs.nova.util.unwrap
import net.minecraft.world.item.ItemStack as MojangStack
import org.bukkit.inventory.ItemStack as BukkitStack

internal object RemainingItemPatches : MultiTransformer(
    BannerDuplicateRecipe::class, BookCloningRecipe::class, Recipe::class,
    AbstractFurnaceBlockEntity::class, BrewingStandBlockEntity::class, CraftServer::class
) {
    
    override fun transform() {
        listOf(
            VirtualClassPath[BannerDuplicateRecipe::getRemainingItems],
            VirtualClassPath[BookCloningRecipe::getRemainingItems],
            VirtualClassPath[BREWING_STAND_BLOCK_ENTITY_DO_BREW_METHOD]
        ).forEach(::patchUsualGetAndConstructNewStackPattern)
        
        patchRecipeGetRemainingItems()
        patchAbstractFurnaceBlockEntityServerTick()
    }
    
    /**
     * Replaces ItemStack.getItem().getCraftingRemainingItem() with RemainingItemStackPatches.getCraftingRemainingItem(ItemStack)
     *
     * Target instructions:
     * new net/minecraft/world/item/ItemStack
     * [...]
     * invokespecial net/minecraft/world/item/ItemStack.<init> (Lnet/minecraft/world/level/ItemLike;)V
     */
    private fun patchUsualGetAndConstructNewStackPattern(node: MethodNode) {
        node.localVariables.clear()
        
        patchHasCraftingRemainingItem(node)
        
        node.replaceEveryRange(
            { it.opcode == Opcodes.NEW && (it as TypeInsnNode).isClass(MojangStack::class) },
            { it.opcode == Opcodes.INVOKESPECIAL && (it as MethodInsnNode).calls(ITEM_STACK_CONSTRUCTOR) },
            0, 0,
            {
                aLoad(4)
                invokeStatic(::getRemainingItemStack)
            }
        )
    }
    
    /**
     * Replaces ItemStack.getItem().getCraftingRemainingItem() with RemainingItemStackPatches.getCraftingRemainingItem(ItemStack)
     *
     * Target instructions:
     * invokevirtual net/minecraft/world/item/ItemStack.getItem()Lnet/minecraft/world/item/Item;
     * invokevirtual net/minecraft/world/item/Item.hasCraftingRemainingItem()Z
     */
    private fun patchHasCraftingRemainingItem(node: MethodNode) {
        node.replaceEvery(1, 0, {
            invokeStatic(::hasCraftingRemainingItem)
        }) { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(Item::hasCraftingRemainingItem) }
    }
    
    /**
     * Replaces this code: https://i.imgur.com/S5Ir112.png with a call to [getRemainingItemStack].
     * Range: https://i.imgur.com/jA78A3s.png
     */
    private fun patchRecipeGetRemainingItems() {
        val methodNode = VirtualClassPath[RECIPE_GET_REMAINING_ITEMS_METHOD]
        methodNode.localVariables.clear()
        methodNode.replaceEveryRange(
            { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(MojangStack::getItem) },
            { it.opcode == Opcodes.INVOKESPECIAL && (it as MethodInsnNode).calls(ITEM_STACK_CONSTRUCTOR) },
            0, 0,
            {
                invokeStatic(::getRemainingItemStack)
                aLoad(2) // NonNullList
                swap()
                iLoad(3) // for loop index
                swap()
            }
        )
    }
    
    /**
     * Replaces this code: https://i.imgur.com/YeHuixV.png with a call to [getRemainingItemStack].
     */
    private fun patchAbstractFurnaceBlockEntityServerTick() {
        val methodNode = VirtualClassPath[AbstractFurnaceBlockEntity::serverTick]
        methodNode.localVariables.clear()
        methodNode.replaceEveryRange(
            { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(Item::getCraftingRemainingItem) },
            { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(NON_NULL_LIST_SET_METHOD) },
            1, -1,
            {
                aLoad(3) // AbstractFurnaceBlockEntity
                getField(ABSTRACT_FURNACE_BLOCK_ENTITY_ITEMS_FIELD)
                aLoad(6) // ItemStack
                invokeStatic(::getRemainingItemStack)
                ldc(1) // slot
                swap()
            }
        )
    }
    
    @JvmStatic
    fun hasCraftingRemainingItem(itemStack: MojangStack): Boolean {
        val novaItem = itemStack.novaItem
        if (novaItem != null)
            return novaItem.craftingRemainingItem != null
        
        return itemStack.item.hasCraftingRemainingItem()
    }
    
    @JvmStatic
    fun getRemainingItemStack(itemStack: MojangStack): MojangStack {
        val novaItem = itemStack.novaItem
        if (novaItem != null)
            return novaItem.craftingRemainingItem?.let { it.unwrap().copy() } ?: MojangStack.EMPTY
        
        // retrieve item directly from field as count = 0 causes getItem to return air
        val item = ReflectionRegistry.ITEM_STACK_ITEM_FIELD.get(itemStack) as Item?
        return item?.craftingRemainingItem?.let(::MojangStack) ?: MojangStack.EMPTY
    }
    
    @JvmStatic
    fun getRemainingBukkitItemStack(itemStack: MojangStack): BukkitStack? {
        val novaItem = itemStack.novaItem
        if (novaItem != null)
            return novaItem.craftingRemainingItem
        
        return itemStack.item.craftingRemainingItem?.let { ItemStack(CraftMagicNumbers.getMaterial(it)) }
    }
    
}