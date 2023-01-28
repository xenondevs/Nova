package xyz.xenondevs.nova.ui.menu.item.recipes.group

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.SmithingRecipe
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.builder.GuiBuilder
import xyz.xenondevs.invui.gui.builder.guitype.GuiType
import xyz.xenondevs.invui.item.ItemWrapper
import xyz.xenondevs.nova.ui.menu.item.recipes.createRecipeChoiceItem
import xyz.xenondevs.nova.ui.overlay.character.gui.CoreGuiTexture

internal object SmithingRecipeGroup : RecipeGroup<SmithingRecipe>() {
    
    override val priority = 3
    override val texture = CoreGuiTexture.RECIPE_SMITHING
    override val icon = ItemWrapper(ItemStack(Material.SMITHING_TABLE))
    
    override fun createGui(recipe: SmithingRecipe): Gui {
        return GuiBuilder(GuiType.NORMAL)
            .setStructure(
                ". . . . . . . . .",
                ". . 1 . 2 . . r .",
                ". . . . . . . . ."
            )
            .addIngredient('1', createRecipeChoiceItem(recipe.base))
            .addIngredient('2', createRecipeChoiceItem(recipe.addition))
            .addIngredient('r', createRecipeChoiceItem(listOf(recipe.result)))
            .build()
    }
    
}