package xyz.xenondevs.nova.ui.menu.explorer.recipes

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.dsl.tabGui
import xyz.xenondevs.invui.dsl.window
import xyz.xenondevs.invui.gui.pageCountProvider
import xyz.xenondevs.invui.gui.pageProvider
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.ui.menu.explorer.ItemsMenu
import xyz.xenondevs.nova.ui.menu.explorer.recipes.group.RecipeGroup
import xyz.xenondevs.nova.util.PlayerMapManager
import xyz.xenondevs.nova.util.component.adventure.appendCentered
import xyz.xenondevs.nova.util.component.adventure.font
import xyz.xenondevs.nova.util.component.adventure.moveToCenter
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.recipe.RecipeContainer
import xyz.xenondevs.nova.world.item.recipe.RecipeRegistry
import java.util.*

/**
 * Tries to open the recipe explorer for the recipes of [item],
 * and returns whether the attempt was successful, i.e. if there were any recipes for the item.
 */
fun Player.showRecipes(item: ItemStack): Boolean =
    showRecipes(ItemUtils.getId(item).toString())

/**
 * Tries to open the recipe explorer for the recipes for [id],
 * and returns whether the attempt was successful, i.e. if there were any recipes for the item.
 */
fun Player.showRecipes(id: String): Boolean =
    showRecipes(id, "creation", RecipeRegistry.CREATION_RECIPES::get, RecipeRegistry.creationInfo::get)

/**
 * Tries to open the recipe explorer for the usages of [item],
 * and returns whether the attempt was successful, i.e. if there were any usages for the item.
 */
fun Player.showUsages(item: ItemStack): Boolean =
    showUsages(ItemUtils.getId(item).toString())

/**
 * Tries to open the recipe explorer for the usages for [id],
 * and returns whether the attempt was successful, i.e. if there were any usages for the item.
 */
fun Player.showUsages(id: String): Boolean =
    showRecipes(id, "usage", RecipeRegistry.USAGE_RECIPES::get, RecipeRegistry.creationInfo::get)

private fun Player.showRecipes(
    id: String,
    partialIdentifier: String,
    getRecipes: (String) -> Map<RecipeGroup<*>, Iterable<RecipeContainer>>?,
    getInfo: (String) -> String?
): Boolean {
    val identifier = "$partialIdentifier:$id"
    if (RecipesMenu.isCurrentlyOpen(this, identifier))
        return false
    
    val recipes = getRecipes(id)
    val info = getInfo(id)
    if (recipes != null) {
        RecipesMenu(this, identifier, recipes, info).open()
        return true
    } else if (info != null) {
        closeInventory()
        sendMessage(Component.translatable(info))
        return true
    }
    return false
}

internal class RecipesMenu(
    private val player: Player,
    private val identifier: String,
    recipes: Map<RecipeGroup<*>, Iterable<RecipeContainer>>,
    info: String? = null
) {
    
    private val window = window(player) {
        val recipes = recipes.toList()
        val tab = mutableProvider(0)
        
        upperGui by tabGui(
            "b p p p p p p p .",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            ". . . . . . . . ."
        ) {
            'b' by backButton()
            
            // tab buttons
            'p' by pagedItemsGui("< x x x x x >") {
                '<' by tabPageBackItem(page, pageCount)
                '>' by tabPageForwardItem(page, pageCount)
                content by buildList {
                    addAll(recipes.mapIndexed { i, (group, _) -> recipeGroupTabButton(i, group, tab) })
                    if (info != null)
                        add(itemInfoButton(info))
                }
            }
            
            // tab content
            this.tab by tab
            tabs by recipes.map { (group, recipes) ->
                pagedGuisGui(
                    "< . . . . . . . >",
                    "x x x x x x x x x",
                    "x x x x x x x x x",
                    "x x x x x x x x x"
                ) {
                    '<' by recipePageBackButton(page)
                    '>' by recipePageForwardButton(page, pageCount)
                    content by recipes.map { container ->
                        @Suppress("UNCHECKED_CAST")
                        group as RecipeGroup<Any>
                        group.getGui(container.recipe)
                    }
                }
            }
            
            val activePage = activeTab.flatMap { it?.pageProvider ?: provider(0) }
            val activePageCount = activeTab.flatMap { it?.pageCountProvider ?: provider(0) }
            title by combinedProvider(tab, activePage, activePageCount) { tab, activePage, activePageCount ->
                val pageNumberString = "${activePage + 1} / $activePageCount"
                val pageNumberComponent = Component.text(pageNumberString, NamedTextColor.WHITE).font("nova:recipes_numbers")
                Component.text()
                    .append(recipes[tab].first.texture.component)
                    .moveToCenter()
                    .appendCentered(pageNumberComponent)
                    .build()
            }
        }
        
        onOpen { active[player] = this@RecipesMenu }
        onClose { active.remove(player, this@RecipesMenu) }
    }
    
    /**
     * Opens this menu.
     */
    fun open() {
        addToHistory(player, this)
        window.open()
    }
    
    override fun equals(other: Any?): Boolean = other is RecipesMenu && other.identifier == identifier
    override fun hashCode(): Int = identifier.hashCode()
    
    companion object {
        
        private const val HISTORY_SIZE = 10
        private val history: MutableMap<Player, MutableList<RecipesMenu>> = PlayerMapManager.createMap()
        private val active: MutableMap<Player, RecipesMenu> = PlayerMapManager.createMap()
        
        /**
         * Adds [menu] to the history of [player].
         * Should be called when [menu] is opened, i.e. when it is an active window.
         */
        fun addToHistory(player: Player, menu: RecipesMenu) {
            val userHistory = history.getOrPut(player) { LinkedList() }
            if (userHistory.lastOrNull() == menu)
                return
            
            userHistory += menu
            if (userHistory.size >= HISTORY_SIZE)
                userHistory.removeFirst()
        }
        
        /**
         * Removes the currently active menu from the history and returns the previous one,
         * or null if there is no previous menu.
         */
        fun popFromHistory(player: Player): RecipesMenu? {
            val userHistory = history[player]
            userHistory?.removeLastOrNull()
            return userHistory?.lastOrNull()
        }
        
        /**
         * Clears the history of [player].
         */
        fun clearHistory(player: Player) {
            history -= player
        }
        
        /**
         * Checks whether a [RecipesMenu] with the given [identifier] is currently opened by [player].
         */
        fun isCurrentlyOpen(player: Player, identifier: String): Boolean {
            return active[player]?.identifier == identifier
        }
        
    }
    
}

private fun backButton(): Item = item {
    itemProvider by DefaultGuiItems.TP_ARROW_LEFT_ON.clientsideProvider
    onClick {
        player.playClickSound()
        RecipesMenu.popFromHistory(player)?.open() ?: ItemsMenu.open(player)
    }
}

private fun recipeGroupTabButton(tab: Int, group: RecipeGroup<*>, activeTab: MutableProvider<Int>): Item = item {
    itemProvider by group.icon
    onClick {
        if (clickType.isLeftClick && activeTab.get() != tab) {
            player.playClickSound()
            activeTab.set(tab)
        } else if (clickType.isRightClick) {
            val identifier = "group:$group"
            val recipes = RecipeRegistry.RECIPES_BY_TYPE[group]
            if (recipes != null && !RecipesMenu.isCurrentlyOpen(player, identifier)) {
                player.playClickSound()
                RecipesMenu(player, identifier, mapOf(group to recipes)).open()
            }
        }
    }
}

private fun itemInfoButton(info: String): Item = item {
    itemProvider by ItemBuilder(Material.KNOWLEDGE_BOOK)
        .setName(Component.translatable("menu.nova.recipe.item_info"))
    onClick {
        if (clickType.isLeftClick) {
            player.playClickSound()
            player.sendMessage(Component.translatable(info))
        }
    }
}

private fun tabPageBackItem(page: MutableProvider<Int>, pageCount: Provider<Int>) = item {
    itemProvider by combinedProvider(page, pageCount) { page, pageCount ->
        if (pageCount <= 1)
            ItemProvider.EMPTY
        else if (page > 0)
            DefaultGuiItems.TP_SMALL_ARROW_LEFT_ON_ALIGNED_RIGHT.clientsideProvider
        else DefaultGuiItems.TP_SMALL_ARROW_LEFT_OFF_ALIGNED_RIGHT.clientsideProvider
    }
    onClick {
        if (clickType.isLeftClick && page.get() > 0) {
            page.set(page.get() - 1)
            player.playClickSound()
        }
    }
}

private fun tabPageForwardItem(page: MutableProvider<Int>, pageCount: Provider<Int>) = item {
    itemProvider by combinedProvider(page, pageCount) { page, pageCount ->
        if (pageCount <= 1)
            ItemProvider.EMPTY
        else if (page + 1 < pageCount)
            DefaultGuiItems.TP_SMALL_ARROW_RIGHT_ON_ALIGNED_LEFT.clientsideProvider
        else DefaultGuiItems.TP_SMALL_ARROW_RIGHT_OFF_ALIGNED_LEFT.clientsideProvider
    }
    onClick {
        if (clickType.isLeftClick && page.get() < pageCount.get() - 1) {
            page.set(page.get() + 1)
            player.playClickSound()
        }
    }
}

private fun recipePageBackButton(page: MutableProvider<Int>): Item = item {
    itemProvider by page.map { page ->
        if (page > 0)
            DefaultGuiItems.TP_ARROW_LEFT_BTN_ON.clientsideProvider
        else DefaultGuiItems.TP_ARROW_LEFT_BTN_OFF.clientsideProvider
    }
    onClick {
        if (clickType.isLeftClick && page.get() > 0) {
            page.set(page.get() - 1)
            player.playClickSound()
        }
    }
}

private fun recipePageForwardButton(page: MutableProvider<Int>, pageCount: Provider<Int>) = item {
    itemProvider by combinedProvider(page, pageCount) { page, pageCount ->
        if (page + 1 < pageCount)
            DefaultGuiItems.TP_ARROW_RIGHT_BTN_ON.clientsideProvider
        else DefaultGuiItems.TP_ARROW_RIGHT_BTN_OFF.clientsideProvider
    }
    onClick {
        if (clickType.isLeftClick && page.get() < pageCount.get() - 1) {
            page.set(page.get() + 1)
            player.playClickSound()
        }
    }
}