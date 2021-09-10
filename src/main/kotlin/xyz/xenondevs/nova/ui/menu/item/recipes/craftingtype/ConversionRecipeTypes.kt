package xyz.xenondevs.nova.ui.menu.item.recipes.craftingtype

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemWrapper
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.Material
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.nova.data.recipe.CustomNovaRecipe
import xyz.xenondevs.nova.data.recipe.RecipeContainer
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.ui.menu.item.recipes.createRecipeChoiceItem
import xyz.xenondevs.nova.ui.overlay.CustomCharacters

abstract class ConversionRecipeType : RecipeType() {
    
    override fun createGUI(holder: RecipeContainer): GUI {
        return if (holder.isSmeltingRecipe) {
            val recipe = holder.recipe as FurnaceRecipe
            createConversionRecipeGUI(recipe.inputChoice, recipe.result, recipe.cookingTime)
        } else {
            val recipe = holder.recipe as CustomNovaRecipe
            createConversionRecipeGUI(recipe.inputStacks, recipe.resultStack, recipe.time)
        }
    }
    
    private fun createConversionRecipeGUI(input: RecipeChoice, result: ItemStack, time: Int): GUI =
        createConversionRecipeGUI(createRecipeChoiceItem(input), result, time)
    
    private fun createConversionRecipeGUI(input: List<ItemStack>, result: ItemStack, time: Int): GUI =
        createConversionRecipeGUI(createRecipeChoiceItem(input), result, time)
    
    private fun createConversionRecipeGUI(inputUIItem: Item, outputItem: ItemStack, time: Int): GUI {
        val builder = GUIBuilder(GUIType.NORMAL, 9, 3)
            .setStructure("" +
                ". . t . . . . . ." +
                ". . i . . . r . ." +
                ". . . . . . . . .")
            .addIngredient('i', inputUIItem)
            .addIngredient('r', createRecipeChoiceItem(listOf(outputItem)))
        
        builder.addIngredient(
            't', NovaMaterialRegistry.STOPWATCH_ICON
            .createBasicItemBuilder()
            .setDisplayName(TranslatableComponent("menu.nova.recipe.time", time / 20.0))
        )
        
        return builder.build()
    }
    
}

object SmeltingRecipeType : ConversionRecipeType() {
    override val priority = 1
    override val icon = ItemWrapper(ItemStack(Material.FURNACE))
    override val overlay = CustomCharacters.FURNACE_RECIPE
}

object PulverizingRecipeType : ConversionRecipeType() {
    override val priority = 2
    override val icon = NovaMaterialRegistry.PULVERIZER.basicItemProvider
    override val overlay = CustomCharacters.PULVERIZER_RECIPE
}

object PressingRecipeType : ConversionRecipeType() {
    override val priority = 3
    override val icon = NovaMaterialRegistry.MECHANICAL_PRESS.basicItemProvider
    override val overlay = CustomCharacters.PRESS_RECIPE
}
