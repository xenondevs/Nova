package xyz.xenondevs.nova.ui.menu.item.recipes.group.hardcoded

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import xyz.xenondevs.nova.data.recipe.RecipeContainer
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.ui.menu.item.recipes.createRecipeChoiceItem
import xyz.xenondevs.nova.ui.menu.item.recipes.group.RecipeGroup
import xyz.xenondevs.nova.ui.overlay.CoreGUITexture

object StarCollectorRecipeGroup : RecipeGroup() {
    
    override val priority = 9
    override val texture = CoreGUITexture.EMPTY_GUI // TODO: Change to the correct texture when available
    override val icon = NovaMaterialRegistry.STAR_COLLECTOR.basicItemProvider
    
    override fun createGUI(container: RecipeContainer): GUI {
        return GUIBuilder(GUIType.NORMAL, 9, 3)
            .setStructure("" +
                ". . . . . . . . ." +
                ". . . . . . . r ." +
                ". . . . . . . . .")
            .addIngredient('r', createRecipeChoiceItem(listOf(container.result!!)))
            .build()
    }
    
}