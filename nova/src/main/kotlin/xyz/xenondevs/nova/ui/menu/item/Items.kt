package xyz.xenondevs.nova.ui.menu.item

import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.NovaItem

/**
 * A list of all button color gui items:
 * - [DefaultGuiItems.RED_BTN]
 * - [DefaultGuiItems.ORANGE_BTN]
 * - [DefaultGuiItems.YELLOW_BTN]
 * - [DefaultGuiItems.GREEN_BTN]
 * - [DefaultGuiItems.BLUE_BTN]
 * - [DefaultGuiItems.PINK_BTN]
 * - [DefaultGuiItems.WHITE_BTN]
 */
val BUTTON_COLORS: List<RegistryEntry<NovaItem>> = listOf(
    DefaultGuiItems.RED_BTN,
    DefaultGuiItems.ORANGE_BTN,
    DefaultGuiItems.YELLOW_BTN,
    DefaultGuiItems.GREEN_BTN,
    DefaultGuiItems.BLUE_BTN,
    DefaultGuiItems.PINK_BTN,
    DefaultGuiItems.WHITE_BTN
)