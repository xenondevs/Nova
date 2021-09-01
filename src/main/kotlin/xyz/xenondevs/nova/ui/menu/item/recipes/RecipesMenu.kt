package xyz.xenondevs.nova.ui.menu.item.recipes

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.gui.impl.PagedGUI
import de.studiocode.invui.gui.impl.SimplePagedNestedGUI
import de.studiocode.invui.gui.impl.SimpleTabGUI
import de.studiocode.invui.gui.impl.TabGUI
import de.studiocode.invui.gui.structure.Structure
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.item.impl.controlitem.ControlItem
import de.studiocode.invui.item.impl.controlitem.TabItem
import de.studiocode.invui.resourcepack.Icon
import de.studiocode.invui.window.Window
import de.studiocode.invui.window.impl.single.SimpleWindow
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.data.recipe.RecipeContainer
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.ui.menu.item.ItemMenu
import xyz.xenondevs.nova.ui.menu.item.recipes.craftingtype.RecipeType
import xyz.xenondevs.nova.ui.overlay.CustomCharacters
import java.util.*

/**
 * A menu that displays the given list of recipes.
 */
class RecipesWindow(player: Player, recipes: Map<RecipeType, Iterable<RecipeContainer>>) : ItemMenu {
    
    private val RECIPES_GUI_STRUCTURE = Structure("" +
        "< . . . . . . . >" +
        "x x x x x x x x x" +
        "x x x x x x x x x" +
        "x x x x x x x x x")
        .addIngredient('<', ::PageBackItem)
        .addIngredient('>', ::PageForwardItem)
    
    private val viewerUUID = player.uniqueId
    
    private lateinit var currentType: RecipeType
    
    private val mainGUI: SimpleTabGUI
    private lateinit var window: Window
    
    init {
        val craftingGUIs: List<Pair<RecipeType, GUI>> = recipes
            .mapValues { (type, holderList) -> PagedRecipesGUI(holderList.map { holder -> type.getGUI(holder) }).gui }
            .map { it.key to it.value }
            .sortedBy { it.first }
        
        val guiBuilder = GUIBuilder(GUIType.TAB, 9, 6)
            .setStructure("" +
                "b . . . . . . . ." +
                "x x x x x x x x x" +
                "x x x x x x x x x" +
                "x x x x x x x x x" +
                "x x x x x x x x x" +
                ". . . . . . . . .")
            .setGUIs(craftingGUIs.map { it.second })
        
        guiBuilder.addIngredient('b', LastRecipeItem(viewerUUID))
        
        mainGUI = guiBuilder.build() as SimpleTabGUI
        
        craftingGUIs
            .map { it.first }
            .withIndex()
            .forEach { (index, craftingType) ->
                if (!::currentType.isInitialized) currentType = craftingType
                mainGUI.setItem(2 + index, CraftingTabItem(craftingType, index))
            }
        
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
    
    inner class CraftingTabItem(private val recipeType: RecipeType, tab: Int) : TabItem(tab) {
        
        override fun getItemProvider(gui: TabGUI) = recipeType.icon
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            super.handleClick(clickType, player, event)
            if (clickType == ClickType.LEFT) {
                currentType = recipeType
                updateTitle()
            }
        }
        
    }
    
    inner class PageBackItem : ControlItem<PagedGUI>() {
        
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
    
    inner class PageForwardItem : ControlItem<PagedGUI>() {
        
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
    
    inner class PagedRecipesGUI(recipes: List<GUI>) {
        
        val isEmpty = recipes.isEmpty()
        val gui: GUI = GUIBuilder(GUIType.PAGED_GUIs, 9, 4)
            .setStructure(RECIPES_GUI_STRUCTURE)
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
