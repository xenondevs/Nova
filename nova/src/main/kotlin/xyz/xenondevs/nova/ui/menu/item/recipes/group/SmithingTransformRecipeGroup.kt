package xyz.xenondevs.nova.ui.menu.item.recipes.group

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.SmithingTransformRecipe
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.item.ItemWrapper
import xyz.xenondevs.nova.ui.menu.item.recipes.createRecipeChoiceItem
import xyz.xenondevs.nova.ui.overlay.guitexture.DefaultGuiTextures

internal object SmithingTransformRecipeGroup : RecipeGroup<SmithingTransformRecipe>() {
    
    override val priority = 3
    override val texture = DefaultGuiTextures.RECIPE_SMITHING
    override val icon = ItemWrapper(ItemStack(Material.SMITHING_TABLE))
    
    override fun createGui(recipe: SmithingTransformRecipe): Gui {
        return Gui.normal()
            .setStructure(
                ". . . . . . . . .",
                ". . 1 2 3 . . r .",
                ". . . . . . . . ."
            )
            .addIngredient('1', createRecipeChoiceItem(recipe.template))
            .addIngredient('2', createRecipeChoiceItem(recipe.base))
            .addIngredient('3', createRecipeChoiceItem(recipe.addition))
            .addIngredient('r', createRecipeChoiceItem(listOf(recipe.result)))
            .build()
    }
    
}