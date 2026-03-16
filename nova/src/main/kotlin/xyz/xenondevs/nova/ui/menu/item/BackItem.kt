package xyz.xenondevs.nova.ui.menu.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.item.AbstractItem
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.clientsideProvider

/**
 * A UI item that opens [previousWindow] when clicked.
 */
fun backItem(
    previousWindow: Provider<Window>,
    itemProvider: Provider<ItemProvider> = DefaultGuiItems.TP_ARROW_LEFT_ON.clientsideProvider,
): Item = item {
    this.itemProvider by itemProvider
    onClick { 
        player.playClickSound()
        previousWindow.get().open()
    }
}

class BackItem(
    private val itemProvider: Provider<ItemProvider> = DefaultGuiItems.ARROW_LEFT_ON.clientsideProvider,
    private val openPrevious: (Player) -> Unit
) : AbstractItem() {
    
    constructor(
        itemProvider: Provider<ItemProvider> = DefaultGuiItems.ARROW_LEFT_ON.clientsideProvider,
        previous: Window
    ) : this(itemProvider, { previous.open() })
    
    constructor(
        itemProvider: ItemProvider,
        openPrevious: (Player) -> Unit
    ) : this(provider(itemProvider), openPrevious)
    
    constructor(
        itemProvider: ItemProvider,
        previous: Window
    ) : this(provider(itemProvider), { previous.open() })
    
    init {
        itemProvider.observeWeak(this) { thisRef -> thisRef.notifyWindows() }
    }
    
    override fun getItemProvider(player: Player) = itemProvider.get()
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        player.playClickSound()
        openPrevious(player)
    }
    
}