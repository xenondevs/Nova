package xyz.xenondevs.nova.ui.menu.recipes

import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.AutoCycleItem
import de.studiocode.invui.item.impl.SimpleItem
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.nova.recipe.RecipeRegistry

fun createRecipeChoiceItem(recipeChoice: RecipeChoice): Item {
    val itemBuilders = if (recipeChoice is RecipeChoice.MaterialChoice) recipeChoice.choices.map(::ItemBuilder)
    else (recipeChoice as RecipeChoice.ExactChoice).choices.map(::ItemBuilder)
    return createRecipeChoiceItem(itemBuilders)
}

@JvmName("createRecipeChoiceItemItemStacks")
fun createRecipeChoiceItem(itemStacks: List<ItemStack>): Item {
    val itemBuilders = itemStacks.map(::ItemBuilder)
    return createRecipeChoiceItem(itemBuilders)
}

@JvmName("createRecipeChoiceItemItemBuilders")
fun createRecipeChoiceItem(itemBuilders: List<ItemBuilder>): Item {
    return if (itemBuilders.size > 1)
        CyclingRecipeChoiceItem(itemBuilders.toTypedArray())
    else StaticRecipeChoiceItem(itemBuilders[0])
}

private fun handleRecipeChoiceClick(player: Player, clickType: ClickType, itemBuilder: ItemBuilder) {
    val name = RecipeRegistry.getNameKey(itemBuilder.build())
    if (clickType == ClickType.LEFT) {
        val recipes = RecipeRegistry.CREATION_RECIPES[name]
        if (recipes?.isNotEmpty() == true) RecipesWindow(player, recipes).show()
    } else if (clickType == ClickType.RIGHT) {
        val recipes = RecipeRegistry.USAGE_RECIPES[name]
        if (recipes?.isNotEmpty() == true) RecipesWindow(player, recipes).show()
    }
}

class CyclingRecipeChoiceItem(itemBuilders: Array<ItemBuilder>) : AutoCycleItem(20, *itemBuilders) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        if (clickType == ClickType.LEFT || clickType == ClickType.RIGHT) handleRecipeChoiceClick(player, clickType, itemBuilder)
    }
    
}

class StaticRecipeChoiceItem(itemBuilder: ItemBuilder) : SimpleItem(itemBuilder) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        if (clickType == ClickType.LEFT || clickType == ClickType.RIGHT) handleRecipeChoiceClick(player, clickType, itemBuilder)
    }
    
}