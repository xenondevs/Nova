package xyz.xenondevs.nova.ui.menu.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.gui.ScrollGui
import xyz.xenondevs.invui.item.AbstractScrollGuiBoundItem
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.clientsideProvider

/**
 * A UI item for scroll guis that decrements [line] on left-click.
 * Uses [on] as item provider if it is possible to scroll up, otherwise uses [off].
 */
fun scrollUpItem(
    line: MutableProvider<Int>,
    on: Provider<ItemProvider> = DefaultGuiItems.TP_ARROW_UP_ON.clientsideProvider,
    off: Provider<ItemProvider> = DefaultGuiItems.TP_ARROW_UP_OFF.clientsideProvider
): Item = scrollUpItem(
    line,
    combinedProvider(
        line, on, off
    ) { line, on, off -> if (line > 0) on else off }
)

/**
 * A UI item for scroll guis that decrements [line] on left-click.
 * Uses [on] as item provider if it is possible to scroll left, otherwise uses [off].
 */
fun scrollLeftItem(
    line: MutableProvider<Int>,
    on: Provider<ItemProvider> = DefaultGuiItems.TP_ARROW_LEFT_ON.clientsideProvider,
    off: Provider<ItemProvider> = DefaultGuiItems.TP_ARROW_LEFT_OFF.clientsideProvider
): Item = scrollUpItem(
    line,
    combinedProvider(
        line, on, off
    ) { line, on, off -> if (line > 0) on else off }
)

/**
 * A UI item for scroll guis that decrements [line] on left-click.
 * Uses [itemProvider] as item provider.
 */
fun scrollUpItem(
    line: MutableProvider<Int>,
    itemProvider: Provider<ItemProvider>
): Item = item {
    this.itemProvider by itemProvider
    onClick {
        if (clickType == ClickType.LEFT && line.get() > 0) {
            player.playClickSound()
            line.set(line.get() - 1)
        }
    }
}

/**
 * A UI item for scroll guis that increments [line] on left-click.
 * Uses [on] as item provider if it is possible to scroll down, otherwise uses [off].
 */
fun scrollDownItem(
    line: MutableProvider<Int>,
    maxLine: Provider<Int>,
    on: Provider<ItemProvider> = DefaultGuiItems.TP_ARROW_DOWN_ON.clientsideProvider,
    off: Provider<ItemProvider> = DefaultGuiItems.TP_ARROW_DOWN_OFF.clientsideProvider
): Item = scrollDownItem(
    line,
    maxLine,
    combinedProvider(
        line, maxLine, on, off
    ) { line, maxLine, on, off -> if (line < maxLine) on else off }
)

/**
 * A UI item for scroll guis that increments [line] on left-click.
 * Uses [on] as item provider if it is possible to scroll right, otherwise uses [off].
 */
fun scrollRightItem(
    line: MutableProvider<Int>,
    maxLine: Provider<Int>,
    on: Provider<ItemProvider> = DefaultGuiItems.TP_ARROW_RIGHT_ON.clientsideProvider,
    off: Provider<ItemProvider> = DefaultGuiItems.TP_ARROW_RIGHT_OFF.clientsideProvider
): Item = scrollDownItem(
    line,
    maxLine,
    combinedProvider(
        line, maxLine, on, off
    ) { line, maxLine, on, off -> if (line < maxLine) on else off }
)

/**
 * A UI item for scroll guis that increments [line] on left-click.
 * Uses [itemProvider] as item provider.
 */
fun scrollDownItem(
    line: MutableProvider<Int>,
    maxLine: Provider<Int>,
    itemProvider: Provider<ItemProvider>
): Item = item {
    this.itemProvider by itemProvider
    onClick {
        if (clickType == ClickType.LEFT && line.get() < maxLine.get()) {
            player.playClickSound()
            line.set(line.get() + 1)
        }
    }
}

/**
 * A UI item for [ScrollGuis][ScrollGui] that scrolls up one line on left-click.
 */
class ScrollUpItem(
    private val on: Provider<ItemProvider> = DefaultGuiItems.ARROW_UP_ON.clientsideProvider,
    private val off: Provider<ItemProvider> = DefaultGuiItems.ARROW_UP_OFF.clientsideProvider
) : AbstractScrollGuiBoundItem() {
    
    constructor(on: ItemProvider, off: ItemProvider) : this(provider(on), provider(off))
    
    init {
        on.observeWeak(this) { thisRef -> thisRef.notifyWindows() }
        off.observeWeak(this) { thisRef -> thisRef.notifyWindows() }
    }
    
    override fun getItemProvider(player: Player): ItemProvider =
        if (gui.line > 0) on.get() else off.get()
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        if (clickType == ClickType.LEFT && gui.line > 0) {
            player.playClickSound()
            gui.line--
        }
    }
    
}

/**
 * A UI item for [ScrollGuis][ScrollGui] that scrolls down one line on left-click.
 */
class ScrollDownItem(
    private val on: Provider<ItemProvider> = DefaultGuiItems.ARROW_DOWN_ON.clientsideProvider,
    private val off: Provider<ItemProvider> = DefaultGuiItems.ARROW_DOWN_OFF.clientsideProvider
) : AbstractScrollGuiBoundItem() {
    
    constructor(on: ItemProvider, off: ItemProvider) : this(provider(on), provider(off))
    
    init {
        on.observeWeak(this) { thisRef -> thisRef.notifyWindows() }
        off.observeWeak(this) { thisRef -> thisRef.notifyWindows() }
    }
    
    override fun getItemProvider(player: Player): ItemProvider =
        if (gui.line < gui.maxLine) on.get() else off.get()
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        if (clickType == ClickType.LEFT && gui.line < gui.maxLine) {
            player.playClickSound()
            gui.line++
        }
    }
    
}

/**
 * A UI item for [ScrollGuis][ScrollGui] that scrolls left one column on left-click.
 */
class ScrollLeftItem(
    private val on: Provider<ItemProvider> = DefaultGuiItems.ARROW_LEFT_ON.clientsideProvider,
    private val off: Provider<ItemProvider> = DefaultGuiItems.ARROW_LEFT_OFF.clientsideProvider
) : AbstractScrollGuiBoundItem() {
    
    constructor(on: ItemProvider, off: ItemProvider) : this(provider(on), provider(off))
    
    init {
        on.observeWeak(this) { thisRef -> thisRef.notifyWindows() }
        off.observeWeak(this) { thisRef -> thisRef.notifyWindows() }
    }
    
    override fun getItemProvider(player: Player): ItemProvider =
        if (gui.line > 0) on.get() else off.get()
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        if (clickType == ClickType.LEFT && gui.line > 0) {
            player.playClickSound()
            gui.line--
        }
    }
    
}

/**
 * A UI item for [ScrollGuis][ScrollGui] that scrolls right one column on left-click.
 */
class ScrollRightItem(
    private val on: Provider<ItemProvider> = DefaultGuiItems.ARROW_RIGHT_ON.clientsideProvider,
    private val off: Provider<ItemProvider> = DefaultGuiItems.ARROW_RIGHT_OFF.clientsideProvider
) : AbstractScrollGuiBoundItem() {
    
    constructor(on: ItemProvider, off: ItemProvider) : this(provider(on), provider(off))
    
    init {
        on.observeWeak(this) { thisRef -> thisRef.notifyWindows() }
        off.observeWeak(this) { thisRef -> thisRef.notifyWindows() }
    }
    
    override fun getItemProvider(player: Player): ItemProvider =
        if (gui.line < gui.maxLine) on.get() else off.get()
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        if (clickType == ClickType.LEFT && gui.line < gui.maxLine) {
            player.playClickSound()
            gui.line++
        }
    }
    
}