package xyz.xenondevs.nova.ui.menu.item

import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.invui.dsl.TabGuiDsl
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.TabGui
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.util.playClickSound

/**
 * A UI item for switching the [active tab][TabGuiDsl.tab] of the gui from the context.
 * 
 * Displays:
 * - [selected] if the tab at [tab] is the current active tab
 * - [on] if the tab at [tab] is available but not active
 * - [off] if the tab at [tab] is not available
 */
context(dsl: TabGuiDsl<*>)
fun tabItem(
    tab: Int,
    selected: Provider<ItemProvider>,
    on: Provider<ItemProvider>,
    off: Provider<ItemProvider>
): Item = tabItem(tab, dsl.tab, dsl.tabs, selected, on, off)

/**
 * A UI item for switching [activeTab] to [tab] in a [TabGui].
 * Uses [tabs] to determine whether the tab at [tab] is available.
 * 
 * Displays:
 * - [selected] if [tab] is the current active tab
 * - [on] if the tab at [tab] is available but not active
 * - [off] if the tab at [tab] is not available
 */
fun tabItem(
    tab: Int,
    activeTab: MutableProvider<Int>,
    tabs: Provider<List<Gui?>>,
    selected: Provider<ItemProvider>,
    on: Provider<ItemProvider>,
    off: Provider<ItemProvider>
): Item = item {
    itemProvider by combinedProvider(
        activeTab, tabs, selected, on, off
    ) { activeTab, tabs, selected, on, off ->
        when {
            activeTab == tab -> selected
            tabs.getOrNull(tab) != null -> on
            else -> off
        }
    }
    onClick {
        if (clickType == ClickType.LEFT
            && tab != activeTab.get()
            && tabs.get().getOrNull(tab) != null
        ) {
            activeTab.set(tab)
            player.playClickSound()
        }
    }
}