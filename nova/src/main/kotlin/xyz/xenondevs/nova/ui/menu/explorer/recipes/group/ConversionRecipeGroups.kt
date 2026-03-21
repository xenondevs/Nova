package xyz.xenondevs.nova.ui.menu.explorer.recipes.group

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.BlastingRecipe
import org.bukkit.inventory.CampfireRecipe
import org.bukkit.inventory.CookingRecipe
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.SmokingRecipe
import org.bukkit.inventory.StonecuttingRecipe
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.invui.dsl.gui
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.set
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemWrapper
import xyz.xenondevs.nova.ui.menu.asUiItem
import xyz.xenondevs.nova.ui.menu.explorer.recipes.createRecipeChoiceItem
import xyz.xenondevs.nova.ui.menu.itemProvider
import xyz.xenondevs.nova.ui.overlay.guitexture.DefaultGuiTextures
import xyz.xenondevs.nova.util.data.getInputStacks
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.recipe.ConversionNovaRecipe

abstract class ConversionRecipeGroup<T : Any> : RecipeGroup<T>() {
    
    override fun createGui(recipe: T): Gui =
        when (recipe) {
            is CookingRecipe<*> -> createConversionRecipeGui(recipe.inputChoice, recipe.result, recipe.cookingTime)
            is StonecuttingRecipe -> createConversionRecipeGui(recipe.inputChoice, recipe.result, 0)
            is ConversionNovaRecipe -> createConversionRecipeGui(recipe.input.getInputStacks(), recipe.result, recipe.time)
            else -> throw UnsupportedOperationException("Unsupported recipe type: ${recipe::class}")
        }
    
    private fun createConversionRecipeGui(input: RecipeChoice, result: ItemStack, time: Int): Gui =
        createConversionRecipeGui(createRecipeChoiceItem(input), result, time)
    
    private fun createConversionRecipeGui(input: List<ItemStack>, result: ItemStack, time: Int): Gui =
        createConversionRecipeGui(createRecipeChoiceItem(input), result, time)
    
    private fun createConversionRecipeGui(inputUiItem: Item, outputItem: ItemStack, time: Int): Gui =
        gui(
            ". . . . . . . . .",
            ". . i . t . r . .",
            ". . . . . . . . ."
        ) {
            'i' by inputUiItem
            'r' by createRecipeChoiceItem(listOf(outputItem))
            
            if (time != 0) {
                't' by itemProvider(DefaultGuiItems.INVISIBLE_ITEM) {
                    name by Component.translatable("menu.nova.recipe.time", Component.text(time / 20.0))
                }
            }
        }
    
}

internal abstract class LitRecipeGroup<T : Any> : ConversionRecipeGroup<T>() {
    override fun createGui(recipe: T): Gui {
        val gui = super.createGui(recipe)
        gui[2, 2] = DefaultGuiItems.TP_LIT_PROGRESS.asUiItem()
        return gui
    }
}

internal object SmeltingRecipeGroup : LitRecipeGroup<FurnaceRecipe>() {
    override val priority = 1
    override val texture = DefaultGuiTextures.RECIPE_CONVERSION
    override val icon = provider { ItemWrapper(ItemStack(Material.FURNACE)) }
}

internal object BlastingRecipeGroup : LitRecipeGroup<BlastingRecipe>() {
    override val priority = 2
    override val texture = DefaultGuiTextures.RECIPE_CONVERSION
    override val icon = provider { ItemWrapper(ItemStack(Material.BLAST_FURNACE)) }
}

internal object SmokingRecipeGroup : LitRecipeGroup<SmokingRecipe>() {
    override val priority = 3
    override val texture = DefaultGuiTextures.RECIPE_CONVERSION
    override val icon = provider { ItemWrapper(ItemStack(Material.SMOKER)) }
}

internal object CampfireRecipeGroup : LitRecipeGroup<CampfireRecipe>() {
    override val priority = 4
    override val texture = DefaultGuiTextures.RECIPE_CONVERSION
    override val icon = provider { ItemWrapper(ItemStack(Material.CAMPFIRE)) }
}

internal object StonecutterRecipeGroup : ConversionRecipeGroup<StonecuttingRecipe>() {
    override val priority = 5
    override val texture = DefaultGuiTextures.RECIPE_CONVERSION
    override val icon = provider { ItemWrapper(ItemStack(Material.STONECUTTER)) }
}
