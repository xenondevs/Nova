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
    3,
    3,
    NonNullList(optimizedRecipe.choiceMatrix.map { it.nmsIngredient }),
    optimizedRecipe.recipe.result.nmsCopy
) {
    
    private val bukkitRecipe = optimizedRecipe.recipe
    
    override fun matches(container: CraftingContainer, level: Level): Boolean {
        // loop over all items in the crafting grid
        return container.width == width && container.height == height &&
            container.contents.withIndex().all { (index, matrixStack) ->
                // check if the item stack matches with the given recipe choice
                val choice = optimizedRecipe.choiceMatrix[index] ?: return@all matrixStack.isEmpty
                return@all matrixStack != null && choice.test(matrixStack.bukkitCopy)
            }
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