package xyz.xenondevs.nova.ui.menu.item.recipes.craftingtype

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.builder.ItemBuilder
import net.md_5.bungee.api.chat.TranslatableComponent
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.recipe.FluidInfuserRecipe
import xyz.xenondevs.nova.data.recipe.RecipeContainer
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.ui.VerticalBar
import xyz.xenondevs.nova.ui.menu.item.recipes.createRecipeChoiceItem
import xyz.xenondevs.nova.ui.overlay.CustomCharacters
import xyz.xenondevs.nova.util.NumberFormatUtils

private val FLUID_CAPACITY = NovaConfig[NovaMaterialRegistry.FLUID_INFUSER].getLong("fluid_capacity")!!

object FluidInfuserRecipeGroup : RecipeGroup() {
    
    override val overlay = CustomCharacters.FLUID_INFUSER
    override val icon = NovaMaterialRegistry.FLUID_INFUSER.basicItemProvider
    override val priority = 4
    
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
        
        val builder = GUIBuilder(GUIType.NORMAL, 9, 3)
            .setStructure("" +
                ". . . t . . . . ." +
                ". . f i . . . r ." +
                ". . . . . . . . .")
            .addIngredient('i', createRecipeChoiceItem(recipe.input))
            .addIngredient('r', createRecipeChoiceItem(listOf(recipe.result)))
            .addIngredient('f', progressItem)
            .addIngredient('t', NovaMaterialRegistry.STOPWATCH_ICON
                .createBasicItemBuilder()
                .setDisplayName(TranslatableComponent("menu.nova.recipe.time", recipe.time / 20.0))
            )
        
        return builder.build().also { FluidBar(recipe.fluidType, recipe.fluidAmount, it, 1, 0, 3) }
    }
    
    private class FluidBar(
        private val type: FluidType,
        private val amount: Long,
        gui: GUI, x: Int, y: Int, height: Int
    ) : VerticalBar(gui, x, y, height) {
        
        override val barMaterial: NovaMaterial
            get() = when (type) {
                FluidType.WATER -> NovaMaterialRegistry.BLUE_BAR_TRANSPARENT
                else -> NovaMaterialRegistry.ORANGE_BAR_TRANSPARENT
            }
        
        init {
            percentage = amount / FLUID_CAPACITY.toDouble()
        }
        
        override fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
            if (amount == Long.MAX_VALUE) {
                itemBuilder.setDisplayName("∞ mB / ∞ mB")
            } else {
                if (FLUID_CAPACITY == Long.MAX_VALUE) itemBuilder.setDisplayName(NumberFormatUtils.getFluidString(amount) + " / ∞ mB")
                else itemBuilder.setDisplayName(NumberFormatUtils.getFluidString(amount, FLUID_CAPACITY))
            }
            return itemBuilder
        }
        
    }
    
}