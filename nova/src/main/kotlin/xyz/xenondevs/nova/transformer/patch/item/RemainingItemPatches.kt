@file:Suppress("unused")

package xyz.xenondevs.nova.transformer.patch.item

import net.minecraft.world.item.crafting.BannerDuplicateRecipe
import net.minecraft.world.item.crafting.BookCloningRecipe
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity
import org.bukkit.craftbukkit.v1_19_R3.CraftServer
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_19_R3.util.CraftMagicNumbers
import org.bukkit.inventory.ItemStack
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.isClass
import xyz.xenondevs.bytebase.util.replaceEvery
import xyz.xenondevs.bytebase.util.replaceEveryRange
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.nmsCopy
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.ABSTRACT_FURNACE_BLOCK_ENTITY_ITEMS_FIELD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.ABSTRACT_FURNACE_BLOCK_ENTITY_SERVER_TICK_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.BANNER_DUPLICATE_RECIPE_GET_REMAINING_ITEMS_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.BOOK_CLONING_RECIPE_GET_REMAINING_ITEMS_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.BREWING_STAND_BLOCK_ENTITY_DO_BREW_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.ITEM_GET_CRAFTING_REMAINING_ITEM_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.ITEM_HAS_CRAFTING_REMAINING_ITEM_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.ITEM_STACK_CONSTRUCTOR
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.ITEM_STACK_GET_ITEM_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.NON_NULL_LIST_SET_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.RECIPE_GET_REMAINING_ITEMS_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import java.io.File
import kotlin.reflect.jvm.javaMethod
import net.minecraft.world.item.ItemStack as MojangStack
import org.bukkit.inventory.ItemStack as BukkitStack

internal object RemainingItemPatches : MultiTransformer(
    setOf(
        BannerDuplicateRecipe::class, BookCloningRecipe::class, Recipe::class,
        AbstractFurnaceBlockEntity::class, BrewingStandBlockEntity::class, CraftServer::class
    ),
    computeFrames = true
) {
    
    override fun transform() {
        listOf(
            VirtualClassPath[BANNER_DUPLICATE_RECIPE_GET_REMAINING_ITEMS_METHOD],
            VirtualClassPath[BOOK_CLONING_RECIPE_GET_REMAINING_ITEMS_METHOD],
            VirtualClassPath[BREWING_STAND_BLOCK_ENTITY_DO_BREW_METHOD]
        ).forEach(::patchUsualGetAndConstructNewStackPattern)
        
        patchRecipeGetRemainingItems()
        patchAbstractFurnaceBlockEntityServerTick()
        patchCraftServerCraftItem()
        
        File("out.class").writeBytes(
            VirtualClassPath[BrewingStandBlockEntity::class].assemble(false)
        )
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
            buildInsnList {
                aLoad(4)
                invokeStatic(ReflectionUtils.getMethodByName(RemainingItemPatches::class, false, "getRemainingItemStack"))
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
        node.replaceEvery(1, 0, buildInsnList {
            invokeStatic(ReflectionUtils.getMethodByName(RemainingItemPatches::class, false, "hasCraftingRemainingItem"))
        }) { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(ITEM_HAS_CRAFTING_REMAINING_ITEM_METHOD) }
    }
    
    /**
     * Replaces this code: https://i.imgur.com/S5Ir112.png with a call to [getRemainingItemStack].
     * Range: https://i.imgur.com/jA78A3s.png
     */
    private fun patchRecipeGetRemainingItems() {
        val methodNode = VirtualClassPath[RECIPE_GET_REMAINING_ITEMS_METHOD]
        methodNode.localVariables.clear()
        methodNode.replaceEveryRange(
            { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(ITEM_STACK_GET_ITEM_METHOD) },
            { it.opcode == Opcodes.INVOKESPECIAL && (it as MethodInsnNode).calls(ITEM_STACK_CONSTRUCTOR) },
            0, 0,
            buildInsnList {
                invokeStatic(ReflectionUtils.getMethodByName(RemainingItemPatches::class, false, "getRemainingItemStack"))
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
        val methodNode = VirtualClassPath[ABSTRACT_FURNACE_BLOCK_ENTITY_SERVER_TICK_METHOD]
        methodNode.localVariables.clear()
        methodNode.replaceEveryRange(
            { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(ITEM_GET_CRAFTING_REMAINING_ITEM_METHOD) },
            { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(NON_NULL_LIST_SET_METHOD) },
            1, -1,
            buildInsnList {
                aLoad(3) // AbstractFurnaceBlockEntity
                getField(ABSTRACT_FURNACE_BLOCK_ENTITY_ITEMS_FIELD)
                aLoad(6) // ItemStack
                invokeStatic(ReflectionUtils.getMethodByName(RemainingItemPatches::class, false, "getRemainingItemStack"))
                ldc(1) // slot
                swap()
            }
        )
    }
    
    /**
     * Replaces this range: https://i.imgur.com/HTZXwHp.png with a call to [getRemainingBukkitItemStack].
     */
    private fun patchCraftServerCraftItem() {
        val methodNode = VirtualClassPath[CraftServer::craftItem.javaMethod!!]
        methodNode.localVariables.clear()
        methodNode.replaceEveryRange(
            { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(ITEM_STACK_GET_ITEM_METHOD) },
            { it.opcode == Opcodes.INVOKESTATIC && (it as MethodInsnNode).calls(CraftItemStack::asBukkitCopy) },
            0, 3,
            buildInsnList {
                invokeStatic(ReflectionUtils.getMethodByName(RemainingItemPatches::class, false, "getRemainingBukkitItemStack"))
                aLoad(1) // ItemStack[]
                swap()
                iLoad(12) // for loop index
                swap()
            }
        )
    }
    
    @JvmStatic
    fun hasCraftingRemainingItem(itemStack: MojangStack): Boolean {
        val novaMaterial = itemStack.novaMaterial
        if (novaMaterial != null)
            return novaMaterial.craftingRemainingItem != null
        
        return itemStack.item.hasCraftingRemainingItem()
    }
    
    @JvmStatic
    fun getRemainingItemStack(itemStack: MojangStack): MojangStack {
        val novaMaterial = itemStack.novaMaterial
        if (novaMaterial != null)
            return novaMaterial.craftingRemainingItem?.get()?.nmsCopy ?: MojangStack.EMPTY
        
        return MojangStack(itemStack.item.craftingRemainingItem)
    }
    
    @JvmStatic
    fun getRemainingBukkitItemStack(itemStack: MojangStack): BukkitStack? {
        val novaMaterial = itemStack.novaMaterial
        if (novaMaterial != null)
            return novaMaterial.craftingRemainingItem?.get()
        
        return itemStack.item.craftingRemainingItem?.let { ItemStack(CraftMagicNumbers.getMaterial(it)) }
    }
    
}