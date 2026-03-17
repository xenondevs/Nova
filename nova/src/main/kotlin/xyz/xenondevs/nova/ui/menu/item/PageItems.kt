package xyz.xenondevs.nova.ui.menu.item

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.dsl.PagedGuiDsl
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.gui.PagedGui
import xyz.xenondevs.invui.item.AbstractPagedGuiBoundItem
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.clientsideProvider

/**
 * A UI item for decrementing the [page][PagedGuiDsl.page] of the gui from the context on left-click.
 * Uses [on] as item provider if it is possible to go back, otherwise uses [off].
 */
context(dsl: PagedGuiDsl<*>)
fun pageBackItem(
    on: Provider<ItemProvider> = DefaultGuiItems.TP_ARROW_LEFT_ON.clientsideProvider,
    off: Provider<ItemProvider> = DefaultGuiItems.TP_ARROW_LEFT_OFF.clientsideProvider
): Item = pageBackItem(dsl.page, on, off)

/**
 * A UI item for decrementing the [page][PagedGuiDsl.page] of the gui from the context on left-click.
 * Uses [itemProvider] as item provider.
 */
context(dsl: PagedGuiDsl<*>)
fun pageBackItem(
    itemProvider: Provider<ItemProvider>
): Item = pageBackItem(dsl.page, itemProvider)

/**
 * A UI item for incrementing the [page][PagedGuiDsl.page] of the gui from the context on left-click.
 * Uses [pageCount][PagedGuiDsl.pageCount] to determine whether it is possible to go forward,
 * then uses [on] if that is the case, otherwise uses [off].
 */
context(dsl: PagedGuiDsl<*>)
fun pageForwardItem(
    on: Provider<ItemProvider> = DefaultGuiItems.TP_ARROW_RIGHT_ON.clientsideProvider,
    off: Provider<ItemProvider> = DefaultGuiItems.TP_ARROW_RIGHT_OFF.clientsideProvider
): Item = pageForwardItem(dsl.page, dsl.pageCount, on, off)

/**
 * A UI item for incrementing the [page][PagedGuiDsl.page] of the gui from the context on left-click.
 * Uses [itemProvider] as item provider.
 */
context(dsl: PagedGuiDsl<*>)
fun pageForwardItem(
    itemProvider: Provider<ItemProvider>
): Item = pageForwardItem(dsl.page, dsl.pageCount, itemProvider)

/**
 * A UI item for paged guis that goes back one page on left-click.
 * Uses [on] as item provider if it is possible to go back, otherwise uses [off].
 */
fun pageBackItem(
    page: MutableProvider<Int>,
    on: Provider<ItemProvider> = DefaultGuiItems.TP_ARROW_LEFT_ON.clientsideProvider,
    off: Provider<ItemProvider> = DefaultGuiItems.TP_ARROW_LEFT_OFF.clientsideProvider
): Item = pageBackItem(
    page,
    combinedProvider(
        page, on, off
    ) { page, on, off -> if (page > 0) on else off }
)

/**
 * A UI item for paged guis that goes back one page on left-click.
 */
fun pageBackItem(
    page: MutableProvider<Int>,
    itemProvider: Provider<ItemProvider>
): Item = item {
    this.itemProvider by itemProvider
    onClick {
        if (clickType == ClickType.LEFT && page.get() > 0) {
            player.playClickSound()
            page.set(page.get() - 1)
        }
    }
}

/**
 * A UI item for paged guis that goes forward one page on left-click.
 * Uses [on] as item provider if it is possible to go forward, otherwise uses [off].
 */
fun pageForwardItem(
    page: MutableProvider<Int>,
    pageCount: Provider<Int>,
    on: Provider<ItemProvider> = DefaultGuiItems.TP_ARROW_RIGHT_ON.clientsideProvider,
    off: Provider<ItemProvider> = DefaultGuiItems.TP_ARROW_RIGHT_OFF.clientsideProvider
): Item = pageForwardItem(
    page,
    pageCount,
    combinedProvider(
        page, pageCount, on, off
    ) { page, pageCount, on, off -> if (page + 1 < pageCount) on else off }
)

/**
 * A UI item for paged guis that goes forward one page on left-click.
 */
fun pageForwardItem(
    page: MutableProvider<Int>,
    pageCount: Provider<Int>,
    itemProvider: Provider<ItemProvider>
): Item = item {
    this.itemProvider by itemProvider
    onClick {
        if (clickType == ClickType.LEFT && page.get() + 1 < pageCount.get()) {
            player.playClickSound()
            page.set(page.get() + 1)
        }
    }
}

/**
 * A UI item for [PagedGuis][PagedGui] that goes back one page on left-click.
 */
class PageBackItem(
    private val on: Provider<ItemProvider> = DefaultGuiItems.ARROW_LEFT_ON.clientsideProvider,
    private val off: Provider<ItemProvider> = DefaultGuiItems.ARROW_LEFT_OFF.clientsideProvider
) : AbstractPagedGuiBoundItem() {
    
    constructor(on: ItemProvider, off: ItemProvider) : this(provider(on), provider(off))
    
    init {
        on.observeWeak(this) { thisRef -> thisRef.notifyWindows() }
        off.observeWeak(this) { thisRef -> thisRef.notifyWindows() }
    }
    
    override fun getItemProvider(player: Player): ItemProvider {
        val itemBuilder = ItemBuilder((if (gui.page > 0) on.get() else off.get()).get())
        itemBuilder.setName(Component.translatable("menu.nova.paged.back", NamedTextColor.GRAY))
        itemBuilder.addLoreLines(
            if (gui.page > 0)
                Component.translatable(
                    "menu.nova.paged.go", NamedTextColor.DARK_GRAY,
                    Component.text(gui.page), Component.text(gui.pageCount)
                )
            else Component.translatable("menu.nova.paged.limit_min", NamedTextColor.DARK_GRAY)
        )
        return itemBuilder
    }
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        if (clickType == ClickType.LEFT && gui.page > 0) {
            player.playClickSound()
            gui.page--
        }
    }
    
}

/**
 * A UI item for [PagedGuis][PagedGui] that goes forward one page on left-click.
 */
class PageForwardItem(
    private val on: Provider<ItemProvider> = DefaultGuiItems.ARROW_RIGHT_ON.clientsideProvider,
    private val off: Provider<ItemProvider> = DefaultGuiItems.ARROW_RIGHT_OFF.clientsideProvider
) : AbstractPagedGuiBoundItem() {
    
    constructor(on: ItemProvider, off: ItemProvider) : this(provider(on), provider(off))
    
    init {
        on.observeWeak(this) { thisRef -> thisRef.notifyWindows() }
        off.observeWeak(this) { thisRef -> thisRef.notifyWindows() }
    }
    
    override fun getItemProvider(player: Player): ItemProvider {
        val itemBuilder = ItemBuilder((if (gui.page + 1 < gui.pageCount) on.get() else off.get()).get())
        itemBuilder.setName(Component.translatable("menu.nova.paged.forward", NamedTextColor.GRAY))
        itemBuilder.addLoreLines(
            if (gui.page + 1 < gui.pageCount)
                Component.translatable(
                    "menu.nova.paged.go", NamedTextColor.DARK_GRAY,
                    Component.text(gui.page + 2), Component.text(gui.pageCount)
                )
            else Component.translatable("menu.nova.paged.limit_max", NamedTextColor.DARK_GRAY)
        )
        return itemBuilder
    }
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        if (clickType == ClickType.LEFT && gui.page + 1 < gui.pageCount) {
            player.playClickSound()
            gui.page++
        }
    }
    
}