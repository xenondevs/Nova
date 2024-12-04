package xyz.xenondevs.nova.ui.menu.explorer.recipes

import org.bukkit.GameMode
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.invui.item.Click
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.ItemWrapper
import xyz.xenondevs.nova.util.addItemCorrectly
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.item.novaItem

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
fun createRecipeChoiceItem(itemProviders: List<ItemProvider>): Item =
    Item.builder()
        .setCyclingItemProvider(20, itemProviders)
        .addClickHandler { item, click -> handleRecipeChoiceItemClick(item, click) }
        .build()

internal fun handleRecipeChoiceItemClick(item: Item, click: Click) {
    val player = click.player
    val clickType = click.clickType
    val itemProvider = item.getItemProvider(player)
    
    val id = ItemUtils.getId(itemProvider.get())
    if (clickType == ClickType.LEFT) {
        player.showRecipes(id)
    } else if (clickType == ClickType.RIGHT) {
        player.showUsages(id)
    } else if (player.gameMode == GameMode.CREATIVE) {
        val itemStack = itemProvider.get().clone().apply {
            amount = novaItem?.maxStackSize ?: type.maxStackSize
        }
        
        if (clickType == ClickType.MIDDLE) {
            player.setItemOnCursor(itemStack)
        } else if (clickType.isShiftClick) {
            player.inventory.addItemCorrectly(itemStack)
        } else if (clickType == ClickType.NUMBER_KEY) {
            player.inventory.setItem(click.hotbarButton, itemStack)
        }
    }
}