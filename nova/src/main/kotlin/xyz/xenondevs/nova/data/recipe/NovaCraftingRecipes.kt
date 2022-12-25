package xyz.xenondevs.nova.data.recipe

import net.minecraft.world.Container
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.crafting.ShapedRecipe
import net.minecraft.world.item.crafting.ShapelessRecipe
import net.minecraft.world.item.crafting.SmeltingRecipe
import net.minecraft.world.level.Level
import org.bukkit.Material.*
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.nova.util.NonNullList
import xyz.xenondevs.nova.util.bukkitCopy
import xyz.xenondevs.nova.util.data.nmsCategory
import xyz.xenondevs.nova.util.data.nmsIngredient
import xyz.xenondevs.nova.util.nmsCopy
import xyz.xenondevs.nova.util.removeFirstWhere
import xyz.xenondevs.nova.util.resourceLocation
import org.bukkit.inventory.FurnaceRecipe as BukkitFurnaceRecipe
import org.bukkit.inventory.ShapedRecipe as BukkitShapedRecipe
import org.bukkit.inventory.ShapelessRecipe as BukkitShapelessRecipe

internal class NovaShapedRecipe(private val optimizedRecipe: OptimizedShapedRecipe) : ShapedRecipe(
    optimizedRecipe.recipe.key.resourceLocation,
    "",
    optimizedRecipe.recipe.category.nmsCategory,
    optimizedRecipe.recipe.shape[0].length,
    optimizedRecipe.recipe.shape.size,
    NonNullList(optimizedRecipe.choiceMatrix.map { it.nmsIngredient }),
    optimizedRecipe.recipe.result.nmsCopy
) {
    private val bukkitRecipe = optimizedRecipe.recipe

    override fun matches(inventorycrafting: CraftingContainer, world: Level?): Boolean {
        for (i in 0..inventorycrafting.width - width) {
            for (j in 0..inventorycrafting.height - height) {
                if (matches(inventorycrafting, i, j, true)
                    || matches(inventorycrafting, i, j, true)
                ) {
                    return true
                }
            }
        }
        return false
    }

    private fun matches(inventorycrafting: CraftingContainer, i: Int, j: Int, inverted: Boolean): Boolean {
        for (k in 0 until inventorycrafting.width) {
            for (l in 0 until inventorycrafting.height) {
                val i1 = k - i
                val j1 = l - j
                var recipeitemstack: RecipeChoice? = null
                if (i1 >= 0 && j1 >= 0 && i1 < width && j1 < height) {
                    recipeitemstack = if (inverted)
                        optimizedRecipe.choiceMatrix[width - i1 - 1 + j1 * width]
                    else
                        optimizedRecipe.choiceMatrix[i1 + j1 * width]

                }
                val item = inventorycrafting.getItem(k + l * inventorycrafting.width).bukkitCopy
                if (!((recipeitemstack == null && item.type == AIR) || recipeitemstack?.test(item) == true)) {
                    return false
                }
            }
        }
        return true
    }
    override fun toBukkitRecipe(): BukkitShapedRecipe {
        return bukkitRecipe
    }
    
}

internal class NovaShapelessRecipe(private val bukkitRecipe: BukkitShapelessRecipe) : ShapelessRecipe(
    bukkitRecipe.key.resourceLocation,
    "",
    bukkitRecipe.category.nmsCategory,
    bukkitRecipe.result.nmsCopy,
    NonNullList(bukkitRecipe.choiceList.map { it.nmsIngredient })
) {
    
    override fun matches(container: CraftingContainer, level: Level): Boolean {
        val choiceList = bukkitRecipe.choiceList
        
        // loop over all items in the inventory and remove matching choices from the choice list
        // if there is an item stack that does not have a matching choice or the choice list is not empty
        // at the end of the loop, the recipe doesn't match
        return container.contents.filterNot { it.isEmpty }.all { matrixStack ->
            choiceList.removeFirstWhere { it.test(matrixStack.bukkitCopy) }
        } && choiceList.isEmpty()
    }
    
    override fun toBukkitRecipe(): org.bukkit.inventory.ShapelessRecipe {
        return bukkitRecipe
    }
    
}

internal class NovaFurnaceRecipe(private val bukkitRecipe: BukkitFurnaceRecipe) : SmeltingRecipe(
    bukkitRecipe.key.resourceLocation,
    "",
    bukkitRecipe.category.nmsCategory,
    bukkitRecipe.inputChoice.nmsIngredient,
    bukkitRecipe.result.nmsCopy,
    bukkitRecipe.experience,
    bukkitRecipe.cookingTime
) {
    
    private val choice = bukkitRecipe.inputChoice
    
    override fun matches(container: Container, level: Level): Boolean {
        return choice.test(container.getItem(0).bukkitCopy)
    }
    
    override fun toBukkitRecipe(): Recipe {
        return bukkitRecipe
    }
    
}