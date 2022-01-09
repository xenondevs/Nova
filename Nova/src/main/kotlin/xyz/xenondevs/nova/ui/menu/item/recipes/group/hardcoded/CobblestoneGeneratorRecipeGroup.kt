package xyz.xenondevs.nova.ui.menu.item.recipes.group.hardcoded

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import net.md_5.bungee.api.chat.TranslatableComponent
import xyz.xenondevs.nova.data.recipe.RecipeContainer
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.ui.StaticFluidBar
import xyz.xenondevs.nova.ui.menu.item.recipes.createRecipeChoiceItem
import xyz.xenondevs.nova.ui.menu.item.recipes.group.RecipeGroup
import xyz.xenondevs.nova.ui.overlay.CustomCharacters

object CobblestoneGeneratorRecipeGroup : RecipeGroup() {
    
    override val priority = 7
    override val overlay = CustomCharacters.COBBLESTONE_GENERATOR
    override val icon = NovaMaterialRegistry.COBBLESTONE_GENERATOR.basicItemProvider
    
    override fun createGUI(container: RecipeContainer): GUI {
        val recipe = container.recipe as CobblestoneGeneratorRecipe
        
        val progressItem = NovaMaterialRegistry.FLUID_PROGRESS_LEFT_RIGHT_TRANSPARENT
            .createBasicItemBuilder()
            .setDisplayName(TranslatableComponent("menu.nova.recipe.cobblestone_generator.${recipe.mode.name.lowercase()}"))
        
        return GUIBuilder(GUIType.NORMAL, 9, 3)
            .setStructure("" +
                ". w l . . . . . ." +
                ". w l . > . r . ." +
                ". w l . m . . . .")
            .addIngredient('r', createRecipeChoiceItem(listOf(container.result!!)))
            .addIngredient('m', recipe.mode.uiItem.itemProvider)
            .addIngredient('>', progressItem)
            .addIngredient('w', StaticFluidBar(FluidType.WATER, 1000, 1000, 3))
            .addIngredient('l', StaticFluidBar(FluidType.LAVA, 1000, 1000, 3))
            .build()
        
    }
    
}