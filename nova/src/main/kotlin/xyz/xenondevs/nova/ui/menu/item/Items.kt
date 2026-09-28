package xyz.xenondevs.nova.ui.menu.item

import org.bukkit.inventory.ItemType
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.world.item.DefaultGuiItems

/**
 * A list of all button color gui items:
 * - [DefaultGuiItems.RED_BTN]
 * - [DefaultGuiItems.ORANGE_BTN]
 * - [DefaultGuiItems.YELLOW_BTN]
 * - [DefaultGuiItems.GREEN_BTN]
 * - [DefaultGuiItems.BLUE_BTN]
 * - [DefaultGuiItems.PINK_BTN]
 * - [DefaultGuiItems.WHITE_BTN]
 * 
 * The order of this list may change, and more button colors may be added at any time.
 * Do not rely on specific indices of this list.
 */
val BUTTON_COLORS: List<RegistryEntry.Paper<ItemType>> = listOf(
    DefaultGuiItems.RED_BTN,
    DefaultGuiItems.ORANGE_BTN,
    DefaultGuiItems.YELLOW_BTN,
    DefaultGuiItems.GREEN_BTN,
    DefaultGuiItems.BLUE_BTN,
    DefaultGuiItems.PINK_BTN,
    DefaultGuiItems.WHITE_BTN
)

/**
 * A list of all button color gui items with a transparent background:
 * - [DefaultGuiItems.TP_RED_BTN]
 * - [DefaultGuiItems.TP_ORANGE_BTN]
 * - [DefaultGuiItems.TP_YELLOW_BTN]
 * - [DefaultGuiItems.TP_GREEN_BTN]
 * - [DefaultGuiItems.TP_BLUE_BTN]
 * - [DefaultGuiItems.TP_PINK_BTN]
 * - [DefaultGuiItems.TP_WHITE_BTN]
 * 
 * The order of this list may change, and more button colors may be added at any time.
 * Do not rely on specific indices of this list.
 */
val TP_BUTTON_COLORS: List<RegistryEntry.Paper<ItemType>> = listOf(
    DefaultGuiItems.TP_RED_BTN,
    DefaultGuiItems.TP_ORANGE_BTN,
    DefaultGuiItems.TP_YELLOW_BTN,
    DefaultGuiItems.TP_GREEN_BTN,
    DefaultGuiItems.TP_BLUE_BTN,
    DefaultGuiItems.TP_PINK_BTN,
    DefaultGuiItems.TP_WHITE_BTN
)