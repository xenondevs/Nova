package xyz.xenondevs.nova.ui.menu.item.recipes

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.PagedGui
import xyz.xenondevs.invui.gui.TabGui
import xyz.xenondevs.invui.gui.structure.Structure
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.ItemWrapper
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.item.builder.setDisplayName
import xyz.xenondevs.invui.item.impl.AbstractItem
import xyz.xenondevs.invui.item.impl.controlitem.ControlItem
import xyz.xenondevs.invui.item.impl.controlitem.TabItem
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.invui.window.changeTitle
import xyz.xenondevs.invui.window.type.context.setTitle
import xyz.xenondevs.nova.data.recipe.RecipeContainer
import xyz.xenondevs.nova.data.recipe.RecipeRegistry
import xyz.xenondevs.nova.data.resources.CharSizes
import xyz.xenondevs.nova.item.DefaultGuiItems
import xyz.xenondevs.nova.ui.menu.item.ItemMenu
import xyz.xenondevs.nova.ui.menu.item.recipes.group.RecipeGroup
import xyz.xenondevs.nova.util.component.adventure.font
import xyz.xenondevs.nova.util.component.adventure.move
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.playClickSound
import java.util.*

fun Player.showRecipes(item: ItemStack) = showRecipes(ItemUtils.getId(item))

fun Player.showRecipes(id: String): Boolean {
    val recipes = RecipeRegistry.CREATION_RECIPES[id]
    val info = RecipeRegistry.creationInfo[id]
    if (recipes != null) {
        RecipesWindow(this, "recipes:$id".hashCode(), recipes, info).show()
        return true
    } else if (info != null) {
        closeInventory()
        sendMessage(Component.translatable(info))
        return true
    }
    return false
}

fun Player.showUsages(item: ItemStack) = showUsages(ItemUtils.getId(item))

fun Player.showUsages(id: String): Boolean {
    val recipes = RecipeRegistry.USAGE_RECIPES[id]
    val info = RecipeRegistry.usageInfo[id]
    if (recipes != null) {
        RecipesWindow(this, "usages:$id".hashCode(), recipes, info).show()
        return true
    } else if (info != null) {
        closeInventory()
        sendMessage(Component.translatable(info))
        return true
    }
    return false
}

/**
 * A menu that displays the given list of recipes.
 */
private class RecipesWindow(
    private val player: Player,
    private val id: Int,
    recipes: Map<RecipeGroup<*>, Iterable<RecipeContainer>>,
    info: String? = null
) : ItemMenu {
    
    private val recipesGuiStructure = Structure(
        "< . . . . . . . >",
        "x x x x x x x x x",
        "x x x x x x x x x",
        "x x x x x x x x x")
        .addIngredient('<', ::PageBackItem)
        .addIngredient('>', ::PageForwardItem)
    
    private val viewerUUID = player.uniqueId
    
    private lateinit var currentType: RecipeGroup<*>
    
    private val mainGui: TabGui
    private lateinit var window: Window
    
    init {
        @Suppress("UNCHECKED_CAST")
        recipes as Map<RecipeGroup<Any>, Iterable<RecipeContainer>>
        
        val craftingTabs: List<Pair<RecipeGroup<*>, Gui>> = recipes
            .mapValues { (type, containers) -> createPagedRecipesGui(containers.map { container -> type.getGui(container.recipe) }) }
            .map { it.key to it.value }
            .sortedBy { it.first }
        
        mainGui = TabGui.normal()
            .setStructure(
                "b . . . . . . . .",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                ". . . . . . . . ."
            )
            .setTabs(craftingTabs.map { it.second })
            .addIngredient('b', LastRecipeItem(viewerUUID))
            .build()
        
        // Add tab buttons
        var lastTab = -1
        craftingTabs
            .map { it.first }
            .forEach { craftingType ->
                if (!::currentType.isInitialized) currentType = craftingType
                mainGui.setItem(2 + ++lastTab, CraftingTabItem(craftingType, lastTab))
            }
        
        if (info != null) mainGui.setItem(2 + ++lastTab, InfoItem(info))
    }
    
    override fun show() {
        ItemMenu.addToHistory(viewerUUID, this)
        window = Window.single {
            it.setViewer(player)
            it.setTitle(getCurrentTitle())
            it.setGui(mainGui)
        }.apply { open() }
    }
    
    private fun getCurrentTitle(): Component {
        val currentTab = mainGui.tabs[mainGui.currentTab] as PagedGui<*>
        val pageNumberString = "${currentTab.currentPage + 1} / ${currentTab.pageAmount}"
        val pageNumberComponent = Component.text(pageNumberString, NamedTextColor.WHITE).font("nova:recipes_numbers")
        return Component.text()
            .move(-8) // move to side to place overlay
            .append(currentType.texture.component)
            .move(-84) // move back to the middle
            .move(CharSizes.calculateComponentWidth(pageNumberComponent) / -2)
            .append(pageNumberComponent)
            .build()
    }
    
    private fun updateTitle() {
        window.changeTitle(getCurrentTitle())
    }
    
    override fun equals(other: Any?): Boolean {
        return other is RecipesWindow && id == other.id
    }
    
    override fun hashCode(): Int {
        return id
    }
    
    private inner class CraftingTabItem(private val recipeGroup: RecipeGroup<*>, tab: Int) : TabItem(tab) {
        
        override fun getItemProvider(gui: TabGui) = recipeGroup.icon
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            super.handleClick(clickType, player, event)
            if (clickType == ClickType.LEFT) {
                currentType = recipeGroup
                updateTitle()
            } else if (clickType == ClickType.RIGHT) {
                val recipes = RecipeRegistry.RECIPES_BY_TYPE[recipeGroup]
                if (recipes != null) RecipesWindow(player, "group:$recipeGroup".hashCode(), mapOf(recipeGroup to recipes)).show()
            }
        }
        
    }
    
    private class InfoItem(private val info: String) : AbstractItem() {
        
        override fun getItemProvider(): ItemBuilder =
            ItemBuilder(Material.KNOWLEDGE_BOOK)
                .setDisplayName(Component.translatable("menu.nova.recipe.item_info"))
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            player.closeInventory()
            player.sendMessage(Component.translatable(info))
        }
        
    }
    
    private inner class PageBackItem : ControlItem<PagedGui<*>>() {
        
        override fun getItemProvider(gui: PagedGui<*>) =
            (if (gui.hasPreviousPage()) DefaultGuiItems.TP_ARROW_LEFT_BTN_ON else DefaultGuiItems.TP_ARROW_LEFT_BTN_OFF)
                .model.clientsideProvider
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            if (clickType == ClickType.LEFT && gui.hasPreviousPage()) {
                player.playClickSound()
                gui.goBack()
                updateTitle()
            }
        }
        
    }
    
    private inner class PageForwardItem : ControlItem<PagedGui<*>>() {
        
        override fun getItemProvider(gui: PagedGui<*>) =
            (if (gui.hasNextPage()) DefaultGuiItems.TP_ARROW_RIGHT_BTN_ON else DefaultGuiItems.TP_ARROW_RIGHT_BTN_OFF)
                .model.clientsideProvider
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            if (clickType == ClickType.LEFT && gui.hasNextPage()) {
                player.playClickSound()
                gui.goForward()
                updateTitle()
            }
        }
        
    }
    
    private fun createPagedRecipesGui(recipes: List<Gui>): Gui =
        PagedGui.guis()
            .setStructure(recipesGuiStructure)
            .setContent(recipes)
            .build()
    
}

private class LastRecipeItem(private val viewerUUID: UUID) : AbstractItem() {
    
    override fun getItemProvider(): ItemProvider {
        return if (ItemMenu.hasHistory(viewerUUID)) {
            DefaultGuiItems.TP_ARROW_LEFT_ON.model.clientsideProvider
        } else ItemWrapper(ItemStack(Material.AIR))
    }
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        if (clickType == ClickType.LEFT && ItemMenu.hasHistory(viewerUUID)) ItemMenu.showPreviousMenu(viewerUUID)
    }
    
}
