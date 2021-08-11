package xyz.xenondevs.nova.ui.menu.recipes

import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.ItemWrapper
import de.studiocode.invui.item.impl.AutoCycleItem
import de.studiocode.invui.item.impl.SimpleItem
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.nova.recipe.RecipeRegistry

fun createRecipeChoiceItem(recipeChoice: RecipeChoice): Item {
    val itemProviders = if (recipeChoice is RecipeChoice.MaterialChoice) recipeChoice.choices.map { ItemWrapper(ItemStack(it)) }
    else (recipeChoice as RecipeChoice.ExactChoice).choices.map(::ItemWrapper)
    return createRecipeChoiceItem(itemProviders)
}

@JvmName("createRecipeChoiceItemItemStacks")
fun createRecipeChoiceItem(itemStacks: List<ItemStack>): Item {
    val itemProviders = itemStacks.map(::ItemWrapper)
    return createRecipeChoiceItem(itemProviders)
}

@JvmName("createRecipeChoiceItemItemBuilders")
fun createRecipeChoiceItem(itemProviders: List<ItemProvider>): Item {
    return if (itemProviders.size > 1)
        CyclingRecipeChoiceItem(itemProviders.toTypedArray())
    else StaticRecipeChoiceItem(itemProviders[0])
}

private fun handleRecipeChoiceClick(player: Player, clickType: ClickType, itemProvider: ItemProvider) {
    val name = RecipeRegistry.getNameKey(itemProvider.get())
    if (clickType == ClickType.LEFT) {
        val recipes = RecipeRegistry.CREATION_RECIPES[name]
        if (recipes?.isNotEmpty() == true) RecipesWindow(player, recipes).show()
    } else if (clickType == ClickType.RIGHT) {
        val recipes = RecipeRegistry.USAGE_RECIPES[name]
        if (recipes?.isNotEmpty() == true) RecipesWindow(player, recipes).show()
    }
}

class CyclingRecipeChoiceItem(itemProviders: Array<ItemProvider>) : AutoCycleItem(20, *itemProviders) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        if (clickType == ClickType.LEFT || clickType == ClickType.RIGHT) handleRecipeChoiceClick(player, clickType, itemProvider)
    }
    
}

class StaticRecipeChoiceItem(itemProvider: ItemProvider) : SimpleItem(itemProvider) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        if (clickType == ClickType.LEFT || clickType == ClickType.RIGHT) handleRecipeChoiceClick(player, clickType, itemProvider)
    }
    
}