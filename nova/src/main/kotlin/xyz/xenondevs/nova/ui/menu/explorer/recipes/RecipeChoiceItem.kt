package xyz.xenondevs.nova.ui.menu.explorer.recipes

import org.bukkit.GameMode
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.ItemWrapper
import xyz.xenondevs.nova.util.addItemCorrectly
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.playClickSound

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
    val itemProvider = item.getItemProvider(player)
    val id = ItemUtils.getId(itemProvider.get()).toString()
    handleRecipeChoiceClick(id, click)
}

internal fun handleRecipeChoiceClick(id: String, click: Click) {
    val player = click.player
    val clickType = click.clickType
    
    if (clickType == ClickType.LEFT) {
        if (player.showRecipes(id))
            player.playClickSound()
    } else if (clickType == ClickType.RIGHT) {
        if (player.showUsages(id)) 
            player.playClickSound()
    } else if (player.gameMode == GameMode.CREATIVE) {
        val itemStack = ItemUtils.getItemStack(id)
        itemStack.amount = itemStack.maxStackSize
        
        if (clickType == ClickType.MIDDLE) {
            player.setItemOnCursor(itemStack)
        } else if (clickType.isShiftClick) {
            player.inventory.addItemCorrectly(itemStack)
        } else if (clickType == ClickType.NUMBER_KEY) {
            player.inventory.setItem(click.hotbarButton, itemStack)
        }
    }
}