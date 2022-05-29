package xyz.xenondevs.nova.data.recipe

import net.minecraft.world.Container
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.crafting.ShapedRecipe
import net.minecraft.world.item.crafting.ShapelessRecipe
import net.minecraft.world.item.crafting.SmeltingRecipe
import net.minecraft.world.level.Level
import org.bukkit.inventory.Recipe
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.util.data.nmsIngredient
import org.bukkit.inventory.FurnaceRecipe as BukkitFurnaceRecipe
import org.bukkit.inventory.ShapedRecipe as BukkitShapedRecipe
import org.bukkit.inventory.ShapelessRecipe as BukkitShapelessRecipe

class NovaShapedRecipe(private val optimizedRecipe: OptimizedShapedRecipe) : ShapedRecipe(
    optimizedRecipe.recipe.key.resourceLocation,
    "",
    3,
    3,
    NonNullList(optimizedRecipe.choiceMatrix.map { it.nmsIngredient }),
    optimizedRecipe.recipe.result.nmsStack
) {
    
    private val bukkitRecipe = optimizedRecipe.recipe
    
    override fun matches(container: CraftingContainer, level: Level): Boolean {
        // loop over all items in the crafting grid
        return container.width == width && container.height == height &&
            container.contents.withIndex().all { (index, matrixStack) ->
                // check if the item stack matches with the given recipe choice
                val choice = optimizedRecipe.choiceMatrix[index] ?: return@all matrixStack.isEmpty
                return@all matrixStack != null && choice.test(matrixStack.bukkitStack)
            }
    }
    
    override fun toBukkitRecipe(): BukkitShapedRecipe {
        return bukkitRecipe
    }
    
}

class NovaShapelessRecipe(private val bukkitRecipe: BukkitShapelessRecipe) : ShapelessRecipe(
    bukkitRecipe.key.resourceLocation,
    "",
    bukkitRecipe.result.nmsStack,
    NonNullList(bukkitRecipe.choiceList.map { it.nmsIngredient })
) {
    
    override fun matches(container: CraftingContainer, level: Level): Boolean {
        val choiceList = bukkitRecipe.choiceList
        
        // loop over all items in the inventory and remove matching choices from the choice list
        // if there is an item stack that does not have a matching choice or the choice list is not empty
        // at the end of the loop, the recipe doesn't match
        return container.contents.filterNot { it.isEmpty }.all { matrixStack ->
            choiceList.removeFirstWhere { it.test(matrixStack.bukkitStack) }
        } && choiceList.isEmpty()
    }
    
    override fun toBukkitRecipe(): org.bukkit.inventory.ShapelessRecipe {
        return bukkitRecipe
    }
    
}

class NovaFurnaceRecipe(private val bukkitRecipe: BukkitFurnaceRecipe) : SmeltingRecipe(
    bukkitRecipe.key.resourceLocation,
    "",
    bukkitRecipe.inputChoice.nmsIngredient,
    bukkitRecipe.result.nmsStack,
    bukkitRecipe.experience,
    bukkitRecipe.cookingTime
) {
    
    private val choice = bukkitRecipe.inputChoice
    
    override fun matches(container: Container, level: Level): Boolean {
        return choice.test(container.getItem(0).bukkitStack)
    }
    
    override fun toBukkitRecipe(): Recipe {
        return bukkitRecipe
    }
    
}