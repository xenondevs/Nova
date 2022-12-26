package xyz.xenondevs.nova.data.recipe

import net.minecraft.world.Container
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.crafting.ShapedRecipe
import net.minecraft.world.item.crafting.ShapelessRecipe
import net.minecraft.world.item.crafting.SmeltingRecipe
import net.minecraft.world.item.crafting.BlastingRecipe
import net.minecraft.world.item.crafting.SmokingRecipe
import net.minecraft.world.item.crafting.CampfireCookingRecipe
import net.minecraft.world.level.Level
import org.bukkit.inventory.Recipe
import xyz.xenondevs.nova.util.NonNullList
import xyz.xenondevs.nova.util.bukkitCopy
import xyz.xenondevs.nova.util.data.nmsCategory
import xyz.xenondevs.nova.util.data.nmsIngredient
import xyz.xenondevs.nova.util.nmsCopy
import xyz.xenondevs.nova.util.removeFirstWhere
import xyz.xenondevs.nova.util.resourceLocation
import org.bukkit.inventory.FurnaceRecipe as BukkitFurnaceRecipe
import org.bukkit.inventory.BlastingRecipe as BukkitBlastingRecipe
import org.bukkit.inventory.SmokingRecipe as BukkitSmokingRecipe
import org.bukkit.inventory.CampfireRecipe as BukkitCampfireRecipe
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
    
    override fun matches(container: CraftingContainer, world: Level): Boolean {
        // Iterate through all the top-left positions of all possible placements of the recipe shape.
        for (startX in 0..container.width - width) {
            for (startY in 0..container.height - height) {
                // Check if the recipe is valid at that position
                if (matchesAt(container, startX, startY, false)
                    || matchesAt(container, startX, startY, true)) return true
            }
        }
        return false
    }
    
    private fun matchesAt(container: CraftingContainer, x: Int, y: Int, horizontalFlip: Boolean): Boolean {
        for (absX in 0 until container.width)
            for (absY in 0 until container.height) {
                // relX and relY is the position relative to the recipe shape
                val relX = absX - x
                val relY = absY - y
                val item = container.getItem(absX + absY * container.width)
                // If relX and relY are in the shape, it will be the RecipeChoice at that position, or null otherwise
                val choice = if (relX in (0 until width) && relY in (0 until height))
                    optimizedRecipe.choiceMatrix.getOrNull(
                        if (horizontalFlip) width * (relY + 1) - relX - 1
                        else relX + relY * width
                    ) else null
                // If choice is null, treat it as an air RecipeChoice.
                if (choice == null) {
                    if (!item.isEmpty) return false
                } else if (!choice.test(item.bukkitCopy)) return false
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

internal class NovaBlastFurnaceRecipe(private val bukkitRecipe: BukkitBlastingRecipe) : BlastingRecipe(
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

internal class NovaSmokerRecipe(private val bukkitRecipe: BukkitSmokingRecipe) : SmokingRecipe(
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

internal class NovaCampfireRecipe(private val bukkitRecipe: BukkitCampfireRecipe) : CampfireCookingRecipe(
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