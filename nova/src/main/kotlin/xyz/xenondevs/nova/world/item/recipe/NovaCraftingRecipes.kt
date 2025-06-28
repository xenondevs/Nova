package xyz.xenondevs.nova.world.item.recipe

import net.minecraft.core.HolderLookup
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.BlastingRecipe
import net.minecraft.world.item.crafting.CampfireCookingRecipe
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.ShapedRecipe
import net.minecraft.world.item.crafting.ShapedRecipePattern
import net.minecraft.world.item.crafting.ShapelessRecipe
import net.minecraft.world.item.crafting.SingleRecipeInput
import net.minecraft.world.item.crafting.SmeltingRecipe
import net.minecraft.world.item.crafting.SmithingRecipeInput
import net.minecraft.world.item.crafting.SmithingTransformRecipe
import net.minecraft.world.item.crafting.SmokingRecipe
import net.minecraft.world.item.crafting.StonecutterRecipe
import net.minecraft.world.item.crafting.TransmuteResult
import net.minecraft.world.level.Level
import org.bukkit.NamespacedKey
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.commons.collections.removeFirstWhere
import xyz.xenondevs.nova.util.data.nmsCategory
import xyz.xenondevs.nova.util.data.toNmsIngredient
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.unwrap
import java.util.*
import org.bukkit.inventory.BlastingRecipe as BukkitBlastingRecipe
import org.bukkit.inventory.CampfireRecipe as BukkitCampfireRecipe
import org.bukkit.inventory.FurnaceRecipe as BukkitFurnaceRecipe
import org.bukkit.inventory.Recipe as BukkitRecipe
import org.bukkit.inventory.ShapedRecipe as BukkitShapedRecipe
import org.bukkit.inventory.ShapelessRecipe as BukkitShapelessRecipe
import org.bukkit.inventory.SmithingTransformRecipe as BukkitSmithingTransformRecipe
import org.bukkit.inventory.SmokingRecipe as BukkitSmokingRecipe
import org.bukkit.inventory.StonecuttingRecipe as BukkitStonecuttingRecipe

internal class NovaShapedRecipe private constructor(
    private val bukkitRecipe: BukkitShapedRecipe,
    pattern: ShapedRecipePattern,
    result: ItemStack,
    val requiredChoices: List<RecipeChoice>,
    val choiceMatrix: Array<Array<RecipeChoice?>>
) : ShapedRecipe(
    "",
    bukkitRecipe.category.nmsCategory,
    pattern, result
) {
    
    fun getChoice(x: Int, y: Int): RecipeChoice? =
        choiceMatrix.getOrNull(x)?.getOrNull(y)
    
    override fun matches(input: CraftingInput, world: Level): Boolean {
        // Iterate through all the top-left positions of all possible placements of the recipe shape.
        for (startX in 0..input.width() - width) {
            for (startY in 0..input.height() - height) {
                // Check if the recipe is valid at that position
                if (matchesAt(input, startX, startY, false)
                    || matchesAt(input, startX, startY, true)) return true
            }
        }
        return false
    }
    
    private fun matchesAt(input: CraftingInput, x: Int, y: Int, horizontalFlip: Boolean): Boolean {
        for (absX in 0..<input.width())
            for (absY in 0..<input.height()) {
                // relX and relY is the position relative to the recipe shape
                val relX = absX - x
                val relY = absY - y
                val item = input.getItem(absX + absY * input.width())
                // If relX and relY are in the shape, it will be the RecipeChoice at that position, or null otherwise
                val choice = if (relX in (0..<width) && relY in (0..<height)) {
                    getChoice(if (horizontalFlip) width - relX - 1 else relX, relY)
                } else null
                // If choice is null, treat it as an air RecipeChoice.
                if (choice == null) {
                    if (!item.isEmpty) return false
                } else if (!choice.test(item.asBukkitMirror())) return false
            }
        return true
    }
    
    override fun toBukkitRecipe(id: NamespacedKey): BukkitShapedRecipe = bukkitRecipe
    
    companion object {
        
        fun of(recipe: BukkitShapedRecipe): NovaShapedRecipe {
            val width = recipe.shape[0].length
            val height = recipe.shape.size
            
            val flatShape: String = recipe.shape.joinToString("")
            val flatChoices: Array<RecipeChoice?> = Array(flatShape.length) { recipe.choiceMap[flatShape[it]] }
            val requiredChoices: List<RecipeChoice> = recipe.shape.joinToString("").mapNotNull { recipe.choiceMap[it] }
            val choiceMatrix: Array<Array<RecipeChoice?>> = Array(width) { x -> Array(height) { y -> recipe.choiceMap[recipe.shape[y][x]] } }
            
            return NovaShapedRecipe(
                recipe,
                ShapedRecipePattern(
                    width, height,
                    flatChoices.map { it.toNmsIngredient() },
                    Optional.empty()
                ),
                recipe.result.unwrap().copy(),
                requiredChoices,
                choiceMatrix
            )
        }
        
    }
    
}

internal class NovaShapelessRecipe(private val bukkitRecipe: BukkitShapelessRecipe) : ShapelessRecipe(
    "",
    bukkitRecipe.category.nmsCategory,
    bukkitRecipe.result.unwrap().copy(),
    bukkitRecipe.choiceList.map { it.toNmsIngredient() }
) {
    
    val choiceList: List<RecipeChoice> = bukkitRecipe.choiceList
    
    override fun matches(container: CraftingInput, level: Level): Boolean {
        val choiceList = ArrayList(choiceList)
        
        // loop over all items in the inventory and remove matching choices from the choice list
        // if there is an item stack that does not have a matching choice or the choice list is not empty
        // at the end of the loop, the recipe doesn't match
        return container.items().filterNot { it.isEmpty }.all { matrixStack ->
            choiceList.removeFirstWhere { it.test(matrixStack.asBukkitMirror()) }
        } && choiceList.isEmpty()
    }
    
    override fun toBukkitRecipe(id: NamespacedKey): BukkitShapelessRecipe = bukkitRecipe
    
}

internal class NovaFurnaceRecipe(private val bukkitRecipe: BukkitFurnaceRecipe) : SmeltingRecipe(
    "",
    bukkitRecipe.category.nmsCategory,
    bukkitRecipe.inputChoice.toNmsIngredient(),
    bukkitRecipe.result.unwrap().copy(),
    bukkitRecipe.experience,
    bukkitRecipe.cookingTime
) {
    
    private val choice = bukkitRecipe.inputChoice
    
    override fun matches(input: SingleRecipeInput, level: Level): Boolean {
        return choice.test(input.getItem(0).asBukkitMirror())
    }
    
    override fun toBukkitRecipe(id: NamespacedKey): BukkitRecipe = bukkitRecipe
    
}

internal class NovaBlastFurnaceRecipe(private val bukkitRecipe: BukkitBlastingRecipe) : BlastingRecipe(
    "",
    bukkitRecipe.category.nmsCategory,
    bukkitRecipe.inputChoice.toNmsIngredient(),
    bukkitRecipe.result.unwrap().copy(),
    bukkitRecipe.experience,
    bukkitRecipe.cookingTime
) {
    
    private val choice = bukkitRecipe.inputChoice
    
    override fun matches(input: SingleRecipeInput, level: Level): Boolean {
        return choice.test(input.getItem(0).asBukkitMirror())
    }
    
    override fun toBukkitRecipe(id: NamespacedKey): BukkitRecipe = bukkitRecipe
    
}

internal class NovaSmokerRecipe(private val bukkitRecipe: BukkitSmokingRecipe) : SmokingRecipe(
    "",
    bukkitRecipe.category.nmsCategory,
    bukkitRecipe.inputChoice.toNmsIngredient(),
    bukkitRecipe.result.unwrap().copy(),
    bukkitRecipe.experience,
    bukkitRecipe.cookingTime
) {
    
    private val choice = bukkitRecipe.inputChoice
    
    override fun matches(input: SingleRecipeInput, level: Level): Boolean {
        return choice.test(input.getItem(0).asBukkitMirror())
    }
    
    override fun toBukkitRecipe(id: NamespacedKey): BukkitRecipe = bukkitRecipe
    
}

internal class NovaCampfireRecipe(private val bukkitRecipe: BukkitCampfireRecipe) : CampfireCookingRecipe(
    "",
    bukkitRecipe.category.nmsCategory,
    bukkitRecipe.inputChoice.toNmsIngredient(),
    bukkitRecipe.result.unwrap().copy(),
    bukkitRecipe.experience,
    bukkitRecipe.cookingTime
) {
    
    private val choice = bukkitRecipe.inputChoice
    
    override fun matches(input: SingleRecipeInput, level: Level): Boolean {
        return choice.test(input.getItem(0).asBukkitMirror())
    }
    
    override fun toBukkitRecipe(id: NamespacedKey): BukkitRecipe = bukkitRecipe
    
}

internal class NovaStonecutterRecipe(private val bukkitRecipe: BukkitStonecuttingRecipe) : StonecutterRecipe(
    "",
    bukkitRecipe.inputChoice.toNmsIngredient(),
    bukkitRecipe.result.unwrap().copy()
) {
    
    private val choice = bukkitRecipe.inputChoice
    
    override fun matches(input: SingleRecipeInput, level: Level): Boolean {
        return choice.test(input.getItem(0).asBukkitMirror())
    }
    
    override fun toBukkitRecipe(id: NamespacedKey): BukkitRecipe = bukkitRecipe
    
}

internal class NovaSmithingTransformRecipe(private val bukkitRecipe: BukkitSmithingTransformRecipe) : SmithingTransformRecipe(
    Optional.of(bukkitRecipe.template.toNmsIngredient()),
    bukkitRecipe.base.toNmsIngredient(),
    Optional.of(bukkitRecipe.addition.toNmsIngredient()),
    TransmuteResult(bukkitRecipe.result.unwrap().itemHolder, bukkitRecipe.result.amount, bukkitRecipe.result.unwrap().componentsPatch)
) {
    
    private val templateChoice = bukkitRecipe.template
    private val baseChoice = bukkitRecipe.base
    private val additionChoice = bukkitRecipe.addition
    
    override fun matches(input: SmithingRecipeInput, world: Level): Boolean {
        return templateChoice.test(input.getItem(0).asBukkitMirror())
            && baseChoice.test(input.getItem(1).asBukkitMirror())
            && additionChoice.test(input.getItem(2).asBukkitMirror())
    }
    
    override fun assemble(input: SmithingRecipeInput, lookup: HolderLookup.Provider): ItemStack {
        val recipeResult = bukkitRecipe.result.unwrap()
        val mergedPatch = ItemUtils.mergeDataComponentPatches(listOf(input.base.componentsPatch, recipeResult.componentsPatch)) // recipeResult overrides input
        return ItemStack(recipeResult.itemHolder, recipeResult.count, mergedPatch)
    }
    
    override fun toBukkitRecipe(id: NamespacedKey): BukkitRecipe = bukkitRecipe
    
}