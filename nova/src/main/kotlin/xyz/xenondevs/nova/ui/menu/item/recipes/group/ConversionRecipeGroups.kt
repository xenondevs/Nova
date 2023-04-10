package xyz.xenondevs.nova.ui.menu.item.recipes.group

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
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemWrapper
import xyz.xenondevs.invui.item.builder.setDisplayName
import xyz.xenondevs.nova.data.recipe.ConversionNovaRecipe
import xyz.xenondevs.nova.item.DefaultGuiItems
import xyz.xenondevs.nova.ui.menu.item.recipes.createRecipeChoiceItem
import xyz.xenondevs.nova.ui.overlay.character.gui.DefaultGuiTextures
import xyz.xenondevs.nova.util.data.getInputStacks

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
    
    private fun createConversionRecipeGui(inputUIItem: Item, outputItem: ItemStack, time: Int): Gui {
        val builder = Gui.normal()
            .setStructure(
                ". . t . . . . . .",
                ". . i . . . r . .",
                ". . . . . . . . ."
            )
            .addIngredient('i', inputUIItem)
            .addIngredient('r', createRecipeChoiceItem(listOf(outputItem)))
        
        if (time != 0) {
            builder.addIngredient(
                't', DefaultGuiItems.TP_STOPWATCH
                .createClientsideItemBuilder()
                .setDisplayName(Component.translatable("menu.nova.recipe.time", Component.text(time / 20.0)))
            )
        }
        
        return builder.build()
    }
    
}

internal object SmeltingRecipeGroup : ConversionRecipeGroup<FurnaceRecipe>() {
    override val priority = 1
    override val icon = ItemWrapper(ItemStack(Material.FURNACE))
    override val texture = DefaultGuiTextures.RECIPE_SMELTING
}

internal object BlastingRecipeGroup : ConversionRecipeGroup<BlastingRecipe>() {
    override val priority = 2
    override val icon = ItemWrapper(ItemStack(Material.BLAST_FURNACE))
    override val texture = DefaultGuiTextures.RECIPE_SMELTING
}

internal object SmokingRecipeGroup : ConversionRecipeGroup<SmokingRecipe>() {
    override val priority = 3
    override val icon = ItemWrapper(ItemStack(Material.SMOKER))
    override val texture = DefaultGuiTextures.RECIPE_SMELTING
}

internal object CampfireRecipeGroup : ConversionRecipeGroup<CampfireRecipe>() {
    override val priority = 4
    override val icon = ItemWrapper(ItemStack(Material.CAMPFIRE))
    override val texture = DefaultGuiTextures.RECIPE_SMELTING
}

internal object StonecutterRecipeGroup : ConversionRecipeGroup<StonecuttingRecipe>() {
    override val priority = 5
    override val icon = ItemWrapper(ItemStack(Material.STONECUTTER))
    override val texture = DefaultGuiTextures.RECIPE_CONVERSION
}
