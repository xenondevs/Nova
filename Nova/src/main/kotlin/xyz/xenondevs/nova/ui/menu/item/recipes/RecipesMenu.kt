package xyz.xenondevs.nova.ui.menu.item.recipes

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.gui.impl.PagedGUI
import de.studiocode.invui.gui.impl.SimplePagedNestedGUI
import de.studiocode.invui.gui.impl.SimpleTabGUI
import de.studiocode.invui.gui.impl.TabGUI
import de.studiocode.invui.gui.structure.Structure
import de.studiocode.invui.item.builder.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.item.impl.controlitem.ControlItem
import de.studiocode.invui.item.impl.controlitem.TabItem
import de.studiocode.invui.resourcepack.Icon
import de.studiocode.invui.window.Window
import de.studiocode.invui.window.impl.single.SimpleWindow
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.recipe.RecipeContainer
import xyz.xenondevs.nova.data.recipe.RecipeRegistry
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.ui.menu.item.ItemMenu
import xyz.xenondevs.nova.ui.menu.item.recipes.group.RecipeGroup
import xyz.xenondevs.nova.ui.overlay.CustomCharacters
import xyz.xenondevs.nova.util.ItemUtils
import java.util.*

fun Player.showRecipes(item: ItemStack) = showRecipes(ItemUtils.getId(item))

fun Player.showRecipes(id: String): Boolean {
    val recipes = RecipeRegistry.CREATION_RECIPES[id]
    val info = RecipeRegistry.CREATION_INFO[id]
    if (recipes != null) {
        RecipesWindow(this, recipes, info).show()
        return true
    } else if (info != null) {
        closeInventory()
        spigot().sendMessage(TranslatableComponent(info))
        return true
    }
    return false
}

fun Player.showUsages(item: ItemStack) = showUsages(ItemUtils.getId(item))

fun Player.showUsages(id: String): Boolean {
    val recipes = RecipeRegistry.USAGE_RECIPES[id]
    val info = RecipeRegistry.USAGE_INFO[id]
    if (recipes != null) {
        RecipesWindow(this, recipes, info).show()
        return true
    } else if (info != null) {
        closeInventory()
        spigot().sendMessage(TranslatableComponent(info))
        return true
    }
    return false
}

/**
 * A menu that displays the given list of recipes.
 */
private class RecipesWindow(player: Player, recipes: Map<RecipeGroup, Iterable<RecipeContainer>>, info: String? = null) : ItemMenu {
    
    private val recipesGuiStructure = Structure("" +
        "< . . . . . . . >" +
        "x x x x x x x x x" +
        "x x x x x x x x x" +
        "x x x x x x x x x")
        .addIngredient('<', ::PageBackItem)
        .addIngredient('>', ::PageForwardItem)
    
    private val viewerUUID = player.uniqueId
    
    private lateinit var currentType: RecipeGroup
    
    private val mainGUI: SimpleTabGUI
    private lateinit var window: Window
    
    init {
        val craftingTabs: List<Pair<RecipeGroup, GUI>> = recipes
            .mapValues { (type, holderList) -> PagedRecipesGUI(holderList.map { holder -> type.getGUI(holder) }).gui }
            .map { it.key to it.value }
            .sortedBy { it.first }
        
        mainGUI = GUIBuilder(GUIType.TAB, 9, 6)
            .setStructure("" +
                "b . . . . . . . ." +
                "x x x x x x x x x" +
                "x x x x x x x x x" +
                "x x x x x x x x x" +
                "x x x x x x x x x" +
                ". . . . . . . . .")
            .setGUIs(craftingTabs.map { it.second })
            .addIngredient('b', LastRecipeItem(viewerUUID))
            .build()
        
        // Add tab buttons
        var lastTab = -1
        craftingTabs
            .map { it.first }
            .forEach { craftingType ->
                if (!::currentType.isInitialized) currentType = craftingType
                mainGUI.setItem(2 + ++lastTab, CraftingTabItem(craftingType, lastTab))
            }
        
        if (info != null) mainGUI.setItem(2 + ++lastTab, InfoItem(info))
    }
    
    override fun show() {
        ItemMenu.addToHistory(viewerUUID, this)
        window = SimpleWindow(viewerUUID, getCurrentTitle(), mainGUI)
        window.show()
    }
    
    private fun getCurrentTitle(): Array<BaseComponent> {
        val currentTab = mainGUI.tabs[mainGUI.currentTab] as SimplePagedNestedGUI
        val pageNumberString = "${currentTab.currentPageIndex + 1} / ${currentTab.pageAmount}"
        
        return ComponentBuilder()
            .append(CustomCharacters.getMovingComponent(-8)) // move to side to place overlay
            .append(currentType.overlay.component)
            .append(CustomCharacters.getMovingComponent(-84)) // move back to the middle
            .append(CustomCharacters.getMovingComponent((
                CustomCharacters.getStringLength(pageNumberString) // this would be the string length in the default font
                    + pageNumberString.replace(" ", "").length // non-space characters are generally one pixel bigger in this font
                ) / -2 // divided by -2 to center it
            ))
            .append(pageNumberString)
            .font("nova:recipes_numbers")
            .color(ChatColor.WHITE)
            .create()
    }
    
    private fun updateTitle() {
        window.changeTitle(getCurrentTitle())
    }
    
    private inner class CraftingTabItem(private val recipeGroup: RecipeGroup, tab: Int) : TabItem(tab) {
        
        override fun getItemProvider(gui: TabGUI) = recipeGroup.icon
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            super.handleClick(clickType, player, event)
            if (clickType == ClickType.LEFT) {
                currentType = recipeGroup
                updateTitle()
            } else if (clickType == ClickType.RIGHT) {
                val recipes = RecipeRegistry.RECIPES_BY_TYPE[recipeGroup]
                if (recipes != null) RecipesWindow(player, mapOf(recipeGroup to recipes)).show()
            }
        }
        
    }
    
    private class InfoItem(private val info: String) : BaseItem() {
        
        override fun getItemProvider(): ItemBuilder =
            ItemBuilder(Material.KNOWLEDGE_BOOK)
                .setDisplayName(TranslatableComponent("menu.nova.recipe.item_info"))
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            player.closeInventory()
            player.spigot().sendMessage(TranslatableComponent(info))
        }
        
    }
    
    private inner class PageBackItem : ControlItem<PagedGUI>() {
        
        override fun getItemProvider(gui: PagedGUI) =
            (if (gui.hasPageBefore()) NovaMaterialRegistry.ARROW_LEFT_ON_BUTTON else NovaMaterialRegistry.ARROW_LEFT_OFF_BUTTON)
                .createBasicItemBuilder()
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            if (clickType == ClickType.LEFT && gui.hasPageBefore()) {
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                gui.goBack()
                updateTitle()
            }
        }
        
    }
    
    private inner class PageForwardItem : ControlItem<PagedGUI>() {
        
        override fun getItemProvider(gui: PagedGUI) =
            (if (gui.hasNextPage()) NovaMaterialRegistry.ARROW_RIGHT_ON_BUTTON else NovaMaterialRegistry.ARROW_RIGHT_OFF_BUTTON)
                .createBasicItemBuilder()
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            if (clickType == ClickType.LEFT && gui.hasNextPage()) {
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                gui.goForward()
                updateTitle()
            }
        }
        
    }
    
    private inner class PagedRecipesGUI(recipes: List<GUI>) {
        
        val gui: GUI = GUIBuilder(GUIType.PAGED_GUIs, 9, 4)
            .setStructure(recipesGuiStructure)
            .setGUIs(recipes)
            .build()
        
    }
    
}

private class LastRecipeItem(private val viewerUUID: UUID) : BaseItem() {
    
    override fun getItemProvider(): ItemBuilder {
        return if (ItemMenu.hasHistory(viewerUUID)) {
            Icon.LIGHT_ARROW_1_LEFT.itemBuilder
        } else ItemBuilder(Material.AIR)
    }
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        if (clickType == ClickType.LEFT && ItemMenu.hasHistory(viewerUUID)) ItemMenu.showPreviousMenu(viewerUUID)
    }
    
}
