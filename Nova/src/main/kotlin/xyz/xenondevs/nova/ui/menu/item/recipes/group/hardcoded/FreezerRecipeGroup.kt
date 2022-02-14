package xyz.xenondevs.nova.ui.menu.item.recipes.group.hardcoded

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import xyz.xenondevs.nova.data.recipe.RecipeContainer
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.ui.StaticFluidBar
import xyz.xenondevs.nova.ui.menu.item.recipes.createRecipeChoiceItem
import xyz.xenondevs.nova.ui.menu.item.recipes.group.RecipeGroup
import xyz.xenondevs.nova.ui.overlay.CoreGUITexture

object FreezerRecipeGroup : RecipeGroup() {
    
    override val priority = 8
    override val texture = CoreGUITexture.EMPTY_GUI // TODO: Change to the correct texture when available
    override val icon = NovaMaterialRegistry.FREEZER.basicItemProvider
    
    override fun createGUI(container: RecipeContainer): GUI {
        val recipe = container.recipe as FreezerRecipe
        
        return GUIBuilder(GUIType.NORMAL, 9, 3)
            .setStructure("" +
                ". w . . . . . . ." +
                ". w . . . . r . ." +
                ". w . . . . . . .")
            .addIngredient('r', createRecipeChoiceItem(listOf(container.result!!)))
            .addIngredient('w', StaticFluidBar(FluidType.WATER, 1000L * recipe.mode.maxCostMultiplier, 100_000, 3))
            .build()
    }
    
}