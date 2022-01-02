package xyz.xenondevs.nova.ui.menu.item.recipes

import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.ItemWrapper
import de.studiocode.invui.item.impl.AutoCycleItem
import de.studiocode.invui.item.impl.SimpleItem
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.nova.util.ItemUtils
import xyz.xenondevs.nova.util.addItemCorrectly

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

fun handleRecipeChoiceItemClick(player: Player, clickType: ClickType, event: InventoryClickEvent, itemProvider: ItemProvider) {
    val id = ItemUtils.getId(itemProvider.get())
    if (clickType == ClickType.LEFT) {
        player.showRecipes(id)
    } else if (clickType == ClickType.RIGHT) {
        player.showUsages(id)
    } else if (player.gameMode == GameMode.CREATIVE) {
        val itemStack = itemProvider.get().clone().apply { amount = type.maxStackSize }
        if (clickType == ClickType.MIDDLE) {
            player.setItemOnCursor(itemStack)
        } else if (clickType.isShiftClick) {
            player.inventory.addItemCorrectly(itemStack)
        } else if (clickType == ClickType.NUMBER_KEY) {
            player.inventory.setItem(event.hotbarButton, itemStack)
        }
    }
}

class CyclingRecipeChoiceItem(itemProviders: Array<ItemProvider>) : AutoCycleItem(20, *itemProviders) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        handleRecipeChoiceItemClick(player, clickType, event, itemProvider)
    }
    
}

class StaticRecipeChoiceItem(itemProvider: ItemProvider) : SimpleItem(itemProvider) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        handleRecipeChoiceItemClick(player, clickType, event, itemProvider)
    }
    
}