package xyz.xenondevs.nova.ui.menu.item.recipes.group

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.ItemWrapper
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.SmithingRecipe
import xyz.xenondevs.nova.data.recipe.RecipeContainer
import xyz.xenondevs.nova.ui.menu.item.recipes.createRecipeChoiceItem
import xyz.xenondevs.nova.ui.overlay.CoreGUITexture

object SmithingRecipeGroup : RecipeGroup() {
    
    override val priority = 3
    override val texture = CoreGUITexture.RECIPE_SMITHING
    override val icon = ItemWrapper(ItemStack(Material.SMITHING_TABLE))
    
    override fun createGUI(container: RecipeContainer): GUI {
        val recipe = container.recipe as SmithingRecipe
        
        return GUIBuilder(GUIType.NORMAL, 9, 3)
            .setStructure("" +
                ". . . . . . . . ." +
                ". . 1 . 2 . . r ." +
                ". . . . . . . . .")
            .addIngredient('1', createRecipeChoiceItem(recipe.base))
            .addIngredient('2', createRecipeChoiceItem(recipe.addition))
            .addIngredient('r', createRecipeChoiceItem(listOf(recipe.result)))
            .build()
    }
    
}