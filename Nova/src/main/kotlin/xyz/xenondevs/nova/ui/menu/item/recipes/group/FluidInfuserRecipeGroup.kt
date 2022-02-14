package xyz.xenondevs.nova.ui.menu.item.recipes.group

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.builder.ItemBuilder
import net.md_5.bungee.api.chat.TranslatableComponent
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.recipe.FluidInfuserRecipe
import xyz.xenondevs.nova.data.recipe.RecipeContainer
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.ui.StaticFluidBar
import xyz.xenondevs.nova.ui.menu.item.recipes.createRecipeChoiceItem
import xyz.xenondevs.nova.ui.overlay.CoreGUITexture

private val FLUID_CAPACITY = NovaConfig[NovaMaterialRegistry.FLUID_INFUSER].getLong("fluid_capacity")!!

object FluidInfuserRecipeGroup : RecipeGroup() {
    
    override val texture = CoreGUITexture.EMPTY_GUI // TODO: change to the correct texture when possible
    override val icon = NovaMaterialRegistry.FLUID_INFUSER.basicItemProvider
    override val priority = 6
    
    override fun createGUI(container: RecipeContainer): GUI {
        val recipe = container.recipe as FluidInfuserRecipe
        
        val progressItem: ItemBuilder
        val translate: String
        if (recipe.mode == FluidInfuserRecipe.InfuserMode.INSERT) {
            progressItem = NovaMaterialRegistry.FLUID_PROGRESS_LEFT_RIGHT_TRANSPARENT.createItemBuilder()
            translate = "menu.nova.recipe.insert_fluid"
        } else {
            progressItem = NovaMaterialRegistry.FLUID_PROGRESS_RIGHT_LEFT_TRANSPARENT.createItemBuilder()
            translate = "menu.nova.recipe.extract_fluid"
        }
        
        progressItem.setDisplayName(TranslatableComponent(
            translate,
            recipe.fluidAmount,
            TranslatableComponent(recipe.fluidType.localizedName)
        ))
        
        return GUIBuilder(GUIType.NORMAL, 9, 3)
            .setStructure("" +
                ". f . t . . . . ." +
                ". f p i . . . r ." +
                ". f . . . . . . .")
            .addIngredient('i', createRecipeChoiceItem(recipe.input))
            .addIngredient('r', createRecipeChoiceItem(listOf(recipe.result)))
            .addIngredient('p', progressItem)
            .addIngredient('f', StaticFluidBar(recipe.fluidType, recipe.fluidAmount, FLUID_CAPACITY, 3))
            .addIngredient('t', NovaMaterialRegistry.STOPWATCH_ICON
                .createBasicItemBuilder()
                .setDisplayName(TranslatableComponent("menu.nova.recipe.time", recipe.time / 20.0))
            )
            .build()
    }
    
}