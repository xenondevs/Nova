package xyz.xenondevs.nova.ui.menu.recipes.craftingtype

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemBuilder
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.Material
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.overlay.CustomCharacters
import xyz.xenondevs.nova.recipe.ConversionNovaRecipe
import xyz.xenondevs.nova.recipe.RecipeContainer
import xyz.xenondevs.nova.ui.menu.recipes.createRecipeChoiceItem

abstract class ConversionRecipeType : RecipeType() {
    
    override fun createGUI(holder: RecipeContainer): GUI {
        return if (holder.isSmeltingRecipe) {
            val recipe = holder.recipe as FurnaceRecipe
            createConversionRecipeGUI(recipe.inputChoice, recipe.result, recipe.cookingTime)
        } else {
            val recipe = holder.recipe as ConversionNovaRecipe
            createConversionRecipeGUI(recipe.inputStacks, recipe.resultStack)
        }
    }
    
    private fun createConversionRecipeGUI(input: RecipeChoice, result: ItemStack, time: Int): GUI =
        createConversionRecipeGUI(createRecipeChoiceItem(input), result, time)
    
    private fun createConversionRecipeGUI(input: List<ItemStack>, result: ItemStack): GUI =
        createConversionRecipeGUI(createRecipeChoiceItem(input), result, -1)
    
    private fun createConversionRecipeGUI(inputUIItem: Item, outputItem: ItemStack, time: Int): GUI {
        val builder = GUIBuilder(GUIType.NORMAL, 9, 3)
            .setStructure("" +
                ". . t . . . . . ." +
                ". . i . . . r . ." +
                ". . . . . . . . .")
            .addIngredient('i', inputUIItem)
            .addIngredient('r', ItemBuilder(outputItem))
        
        if (time != -1) {
            builder.addIngredient(
                't', NovaMaterial.STOPWATCH_ICON
                .createBasicItemBuilder()
                .setDisplayName(TranslatableComponent("menu.nova.recipe.time", time / 20.0))
            )
        }
        
        return builder.build()
    }
    
}

object SmeltingRecipeType : ConversionRecipeType() {
    override val priority = 1
    override val icon = ItemBuilder(Material.FURNACE)
    override val overlay = CustomCharacters.FURNACE_RECIPE
}

object PulverizingRecipeType : ConversionRecipeType() {
    override val priority = 2
    override val icon = NovaMaterial.PULVERIZER.createBasicItemBuilder()
    override val overlay = CustomCharacters.PULVERIZER_RECIPE
}

object PressingRecipeType : ConversionRecipeType() {
    override val priority = 3
    override val icon = NovaMaterial.MECHANICAL_PRESS.createBasicItemBuilder()
    override val overlay = CustomCharacters.PRESS_RECIPE
}
