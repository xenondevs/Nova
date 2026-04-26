package xyz.xenondevs.nova.ui.menu.item

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.BundleContents.bundleContents
import io.papermc.paper.datacomponent.item.CustomModelData.customModelData
import net.kyori.adventure.key.Key
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.BundleContents
import net.minecraft.world.item.component.TooltipDisplay
import org.bukkit.Tag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import xyz.xenondevs.commons.collections.repeated
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.invui.dsl.ItemDsl
import xyz.xenondevs.invui.dsl.ItemProviderDsl
import xyz.xenondevs.invui.dsl.ScrollGuiDsl
import xyz.xenondevs.invui.dsl.WindowDsl
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.dsl.itemProvider
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.ScrollGui
import xyz.xenondevs.invui.gui.Slot
import xyz.xenondevs.invui.gui.SlotElement
import xyz.xenondevs.invui.gui.SlotElementSupplier
import xyz.xenondevs.invui.inventory.Inventory
import xyz.xenondevs.invui.inventory.get
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.ItemWrapper
import xyz.xenondevs.invui.util.ItemUtils
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.nova.util.item.isNullOrEmpty
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.util.toNmsTemplate
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.clientsideProvider
import xyz.xenondevs.nova.world.item.logic.PacketItems
import java.util.*
import kotlin.math.round
import kotlin.random.Random
import net.minecraft.network.chat.Component as MojangComponent
import net.minecraft.world.item.Item as MojangItem
import net.minecraft.world.item.ItemStack as MojangStack

private const val SLOTS = 3
private const val FIRST_SLOT = 0
private const val LAST_SLOT = SLOTS - 1
private const val SLOT_SIZE = 16
private const val STRETCHED_SLOT_SIZE = 18
private val SCROLLABLE_BUNDLE_CONTENTS = BundleContents(listOfNotNull(ItemUtils.getPlaceholder().toNmsTemplate()).repeated(3))

//<editor-fold desc="legacy scroller">
/**
 * Creates a scroller UI item for changing the [line][ScrollGuiDsl.line] of the gui from the context
 * using the mouse wheel.
 * This is a single non-moving scroller item that uses the bundle fullness bar to indicate the current scroll position.
 */
context(windowDsl: WindowDsl, guiDsl: ScrollGuiDsl<*>)
fun scrollerItem(
    itemProvider: Provider<ItemProvider> = DefaultGuiItems.TP_SCROLLER_VERTICAL.clientsideProvider
): Item = scrollerItem(
    windowDsl.window,
    guiDsl.line,
    guiDsl.maxLine,
    itemProvider
)

/**
 * Creates a scroller UI item that can be used to scroll through a [xyz.xenondevs.invui.gui.ScrollGui]
 * using the mouse wheel.
 * This is a single non-moving scroller item that uses the bundle fullness bar to indicate the current scroll position.
 */
fun scrollerItem(
    window: Provider<Window>,
    line: MutableProvider<Int>,
    maxLine: Provider<Int>,
    itemProvider: Provider<ItemProvider> = DefaultGuiItems.TP_SCROLLER_VERTICAL.clientsideProvider,
): Item = item {
    this.itemProvider by combinedProvider(line, maxLine, itemProvider) { line, maxLine, itemProvider ->
        val progress = (line + 1.0) / (maxLine + 1.0)
        ItemBuilder(ItemUtils.asType(itemProvider.get(), ItemType.BUNDLE))
            .set(DataComponentTypes.BUNDLE_CONTENTS, bundleContents(listOf(
                ItemUtils.getPlaceholder().apply { amount = ((progress * 64).toInt() - 2).coerceIn(1..64) },
                ItemUtils.getPlaceholder(),
                ItemUtils.getPlaceholder()
            )))
            .hideTooltip(true)
            // set random component to make sure the bundle resets every time serverWindowState is updated
            .set(DataComponentTypes.BREAK_SOUND, Key.key(Random.nextInt().toString()))
    }
    
    onBundleSelect {
        handleBundleScroll(window.get(), line, maxLine, bundleSlot)
    }
}
//</editor-fold>

//<editor-fold desc="scroll bar">
/**
 * Creates a scroll bar slot element supplier that can be used to scroll through the [ScrollGui] using the mouse wheel.
 * Depending on the slot layout, the scroll bar will automatically choose between a vertical ([verticalOn], [verticalOff]) or a horizontal ([horizontalOn], [horizontalOff]) design.
 * 
 * Additionally, the scroll bar can be offset by [offset] pixels. 
 * For vertical scroll bars, this will offset on the horizontal axis, and for horizontal scroll bars, this will offset on the vertical axis.
 * 
 * Note that this does not enable mouse wheel scrolling on the content. For that, use:
 * - [installInventoryScrollSupport] + set [SCROLL_ENABLING_VISUALIZER] or [SCROLL_ENABLING_VISUALIZER_EMPTIES] on the inventory via [Inventory.setVisualizer] for `ScrollGui<Inventory>`
 * - [installItemScrollSupport] + a scroll-supporting [ItemProvider] via [scrollableItemProvider] or built from [SCROLLABLE_BASE] for `ScrollGui<Item>`
 * - [installBackgroundScrollSupport] to allow scrolling on empty background slots
 * 
 * If you want to change the design of the scroll bar, override [verticalOn], [verticalOff], [horizontalOn], [horizontalOff] with custom item providers
 * that fulfill the following criteria:
 * 1. `custom_model_data.floats[0]` defines the movement of the scroll bar in pixels.
 * For example, a vertical scroll bar with a value of `1` would be moved down by one pixel.
 * This also needs to support out-of-bounds values where the scroller is not visible anymore.
 * 2. `custom_model_data.floats[1]` defines the [offset] of the scroll bar in pixels.
 * For vertical scroll bars, negative values move it to the right and positive values move it to the left.
 * For horizontal scroll bars, negative values move it upwards and positive values move it downwards.
 * 3. The size of the scroller (for vertical bars: the height; for horizontal bars: the width) must be equal to [size]
 */
context(windowDsl: WindowDsl, guiDsl: ScrollGuiDsl<*>)
fun scrollBar(
    offset: Int = 0,
    verticalOn: Provider<ItemProvider> = DefaultGuiItems.TP_SCROLL_BAR_VERTICAL.clientsideProvider,
    verticalOff: Provider<ItemProvider> = DefaultGuiItems.TP_SCROLL_BAR_VERTICAL_DISABLED.clientsideProvider,
    horizontalOn: Provider<ItemProvider> = DefaultGuiItems.TP_SCROLL_BAR_HORIZONTAL.clientsideProvider,
    horizontalOff: Provider<ItemProvider> = DefaultGuiItems.TP_SCROLL_BAR_HORIZONTAL_DISABLED.clientsideProvider,
    size: Provider<Int> = provider(15),
) = scrollBar(
    offset,
    windowDsl.window,
    guiDsl.line,
    guiDsl.maxLine,
    verticalOn,
    verticalOff,
    horizontalOn,
    horizontalOff,
    size
)

/**
 * Creates a scroll bar slot element supplier that can be used to scroll through the [ScrollGui] using the mouse wheel.
 * Depending on the slot layout, the scroll bar will automatically choose between a vertical ([verticalOn], [verticalOff]) or a horizontal ([horizontalOn], [horizontalOff]) design.
 * 
 * Additionally, the scroll bar can be offset by [offset] pixels. 
 * For vertical scroll bars, this will offset on the horizontal axis, and for horizontal scroll bars, this will offset on the vertical axis.
 * 
 * Note that this does not enable mouse wheel scrolling on the content. For that, use:
 * - [installInventoryScrollSupport] + set [SCROLL_ENABLING_VISUALIZER] or [SCROLL_ENABLING_VISUALIZER_EMPTIES] on the inventory via [Inventory.setVisualizer] for `ScrollGui<Inventory>`
 * - [installItemScrollSupport] + a scroll-supporting [ItemProvider] via [scrollableItemProvider] or built from [SCROLLABLE_BASE] for `ScrollGui<Item>`
 * - [installBackgroundScrollSupport] to allow scrolling on empty background slots
 * 
 * If you want to change the design of the scroll bar, override [verticalOn], [verticalOff], [horizontalOn], [horizontalOff] with custom item providers
 * that fulfill the following criteria:
 * 1. `custom_model_data.floats[0]` defines the movement of the scroll bar in pixels.
 * For example, a vertical scroll bar with a value of `1` would be moved down by one pixel.
 * This also needs to support out-of-bounds values where the scroller is not visible anymore.
 * 2. `custom_model_data.floats[1]` defines the [offset] of the scroll bar in pixels.
 * For vertical scroll bars, negative values move it to the right and positive values move it to the left.
 * For horizontal scroll bars, negative values move it upwards and positive values move it downwards.
 * 3. The size of the scroller (for vertical bars: the height; for horizontal bars: the width) must be equal to [size]
 */
fun scrollBar(
    offset: Int = 0,
    window: Provider<Window>,
    line: MutableProvider<Int>,
    maxLine: Provider<Int>,
    verticalOn: Provider<ItemProvider> = DefaultGuiItems.TP_SCROLL_BAR_VERTICAL.clientsideProvider,
    verticalOff: Provider<ItemProvider> = DefaultGuiItems.TP_SCROLL_BAR_VERTICAL_DISABLED.clientsideProvider,
    horizontalOn: Provider<ItemProvider> = DefaultGuiItems.TP_SCROLL_BAR_HORIZONTAL.clientsideProvider,
    horizontalOff: Provider<ItemProvider> = DefaultGuiItems.TP_SCROLL_BAR_HORIZONTAL_DISABLED.clientsideProvider,
    size: Provider<Int> = provider(15)
) = SlotElementSupplier { slots ->
    require(slots.size > 1) { "Scroll bar needs at least 2 slots" }
    
    val on: Provider<ItemProvider>
    val off: Provider<ItemProvider>
    val from: Int
    val orientation = determineOrientation(slots)
    when (orientation) {
        ScrollGui.LineOrientation.VERTICAL -> {
            on = verticalOn
            off = verticalOff
            from = slots.minOf { it.y }
        }
        
        ScrollGui.LineOrientation.HORIZONTAL -> {
            on = horizontalOn
            off = horizontalOff
            from = slots.minOf { it.x }
        }
        
        null -> throw IllegalArgumentException("Slots need to be in a line")
    }
    
    val sizePx = (slots.size - 1) * STRETCHED_SLOT_SIZE
    
    fun sectionItem(section: Int) = item {
        val fromPx = section * STRETCHED_SLOT_SIZE
        
        itemProvider by itemProvider {
            base by combinedProvider(maxLine, on, off) { maxLine, on, off ->
                val original = (if (maxLine > 0) on.get() else off.get()).unwrap()
                val result = asType(original, PacketItems.SCROLLABLE_ITEM_HOLDER)
                result.set(DataComponents.BUNDLE_CONTENTS, SCROLLABLE_BUNDLE_CONTENTS)
                result.asBukkitMirror()
            }
            data[DataComponentTypes.CUSTOM_MODEL_DATA] by combinedProvider(line, maxLine) { line, maxLine ->
                val targetPx = if (maxLine > 0) {
                    val progress = line.toDouble() / maxLine.toDouble()
                    val extraShift = (SLOT_SIZE - size.get()) * progress
                    line.toDouble() / maxLine.toDouble() * sizePx + extraShift
                } else 0.0
                customModelData()
                    .addFloat((targetPx - fromPx).toFloat())
                    .addFloat(offset.toFloat())
                    // set random float to make sure the item updates every time serverWindowState is updated
                    .addFloat(Random.nextFloat())
                    .build()
            }
        }
        
        onBundleSelect {
            handleBundleScroll(window.get(), line, maxLine, bundleSlot)
        }
        
        onClick {
            val toLine = round(fromPx.toDouble() / sizePx.toDouble() * maxLine.get()).toInt()
            if (line.get() != toLine) {
                line.set(toLine)
                player.playClickSound()
            }
        }
    }
    
    slots.map {
        val to = if (orientation == ScrollGui.LineOrientation.VERTICAL) it.y else it.x
        SlotElement.Item(sectionItem(to - from))
    }
}

/**
 * Returns the orientation of the line that [slots] form, or null if they don't form a line.
 * Throws an exception if [slots] has less than 2 elements.
 */
private fun determineOrientation(slots: List<Slot>): ScrollGui.LineOrientation? {
    require(slots.size > 1)
    
    var continuousX = true
    var continuousY = true
    for (i in 1..slots.lastIndex) {
        if (slots[i].x != slots[i - 1].x)
            continuousX = false
        if (slots[i].y != slots[i - 1].y)
            continuousY = false
    }
    
    if (continuousY)
        return ScrollGui.LineOrientation.HORIZONTAL
    if (continuousX)
        return ScrollGui.LineOrientation.VERTICAL
    return null
}
//</editor-fold>

//<editor-fold desc="scroll support">

//<editor-fold desc="visualizer (uses nms for reduce conversion overhead)">
/**
 * A visualizer (for [Inventory.setVisualizer], [Window.setCursorVisualizer], [SlotElement.InventoryLink.visualizer])
 * that enables mouse-wheel-scrolling via bundle selection (handleable via [Item.handleBundleSelect], [Inventory.bundleSelectHandlers], [Gui.handleBundleSelect]).
 * 
 * Does not enable scrolling on empty item stacks. For that, use [SCROLL_ENABLING_VISUALIZER_EMPTIES].
 * 
 * @see installInventoryScrollSupport
 */
val SCROLL_ENABLING_VISUALIZER: (ItemStack?) -> ItemProvider? = {
    val original = it.unwrap()
    if (!original.isEmpty && !original.typeHolder().value().isBundle()) {
        val result = asType(original, PacketItems.SCROLLABLE_ITEM_HOLDER)
        result.set(DataComponents.BUNDLE_CONTENTS, SCROLLABLE_BUNDLE_CONTENTS)
        // random break sound makes each item unique to ensure that scrolling definitely resets the client-side selected slot to -1
        result.set(DataComponents.NOTE_BLOCK_SOUND, Identifier.withDefaultNamespace(Random.nextInt().toString()))
        ItemWrapper(result.asBukkitMirror())
    } else null
}

/**
 * Template patch for scrollable invisible items.
 */
private val HIDDEN_EMPTY_TEMPLATE = DataComponentPatch.builder()
    .set(DataComponents.ITEM_MODEL, Identifier.withDefaultNamespace("air"))
    .set(DataComponents.ITEM_NAME, MojangComponent.empty())
    .set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay(true, Collections.emptySortedSet()))
    .set(DataComponents.BUNDLE_CONTENTS, SCROLLABLE_BUNDLE_CONTENTS)
    .build()

/**
 * A visualizer (for [Inventory.setVisualizer], [Window.setCursorVisualizer], [SlotElement.InventoryLink.visualizer])
 * that enables mouse-wheel-scrolling via bundle selection (handleable via [Item.handleBundleSelect], [Inventory.bundleSelectHandlers], [Gui.handleBundleSelect]).
 * 
 * Also enables scrolling on empty item stacks.
 * (Note that this breaks item dragging. To keep item dragging, use [SCROLL_ENABLING_VISUALIZER] instead.)
 * 
 * @see installInventoryScrollSupport
 */
val SCROLL_ENABLING_VISUALIZER_EMPTIES: (ItemStack?) -> ItemProvider? = {
    if (it.isNullOrEmpty()) {
        val result = MojangStack(PacketItems.SCROLLABLE_ITEM_HOLDER, 1, HIDDEN_EMPTY_TEMPLATE)
        result.set(DataComponents.NOTE_BLOCK_SOUND, Identifier.withDefaultNamespace(Random.nextInt().toString()))
        ItemWrapper(result.asBukkitMirror())
    } else SCROLL_ENABLING_VISUALIZER(it)
}

/**
 * An [ItemStack] that can be used as a base in e.g. [ItemProviderDsl] that enables mouse-wheel-scrolling via bundle selection
 * (handleable via [Item.handleBundleSelect], [Inventory.bundleSelectHandlers], [Gui.handleBundleSelect]).
 * 
 * Usage:
 * ```
 * item {
 *     itemProvider by itemProvider(SCROLLABLE_BASE) {
 *         name by "Example"
 *     }
 *     installItemScrollSupport()
 * }
 * ```
 * 
 * @see installItemScrollSupport
 */
val SCROLLABLE_BASE: ItemStack
    get() {
        val result = MojangStack(PacketItems.SCROLLABLE_ITEM_HOLDER, 1, HIDDEN_EMPTY_TEMPLATE)
        result.set(DataComponents.NOTE_BLOCK_SOUND, Identifier.withDefaultNamespace(Random.nextInt().toString()))
        return result.asBukkitMirror()
    }

/**
 * Faster version of [ItemUtils.asType] that uses NMS to reduce conversion overhead.
 * This is important as the visualizer is called frequently when scrolling.
 */
private fun asType(original: MojangStack, targetType: Holder<MojangItem>): MojangStack {
    if (original.isEmpty)
        return MojangStack.EMPTY
    
    val result = MojangStack(targetType, original.count)
    for (type in BuiltInRegistries.DATA_COMPONENT_TYPE) {
        val data = original.getTyped(type)
        if (data != null) {
            result.set(data)
        } else {
            result.remove(type)
        }
    }
    return result
}

/**
 * Tag-independently checks whether a [MojangItem] is a bundle.
 * While Nova does not edit the tag server-side, another plugin may.
 * Additionally, this is probably slightly faster.
 */
private fun MojangItem.isBundle(): Boolean =
    this === Items.BUNDLE || this === Items.WHITE_BUNDLE || this === Items.ORANGE_BUNDLE
        || this === Items.MAGENTA_BUNDLE || this === Items.LIGHT_BLUE_BUNDLE || this === Items.YELLOW_BUNDLE
        || this === Items.LIME_BUNDLE || this === Items.PINK_BUNDLE || this === Items.GRAY_BUNDLE
        || this === Items.LIGHT_GRAY_BUNDLE || this === Items.CYAN_BUNDLE || this === Items.PURPLE_BUNDLE
        || this === Items.BLUE_BUNDLE || this === Items.BROWN_BUNDLE || this === Items.GREEN_BUNDLE
        || this === Items.RED_BUNDLE || this === Items.BLACK_BUNDLE
//</editor-fold>

/**
 * Enables mouse-wheel-scroll handling on embedded inventories.
 * This requires the inventories to use [SCROLL_ENABLING_VISUALIZER] or [SCROLL_ENABLING_VISUALIZER_EMPTIES]
 * via [Inventory.setVisualizer]. Otherwise, scrolling will not work!
 * 
 * @see installBackgroundScrollSupport
 */
context(windowDsl: WindowDsl, guiDsl: ScrollGuiDsl<Inventory>)
fun installInventoryScrollSupport() {
    val gui by guiDsl.gui
    val window by windowDsl.window
    
    windowDsl.cursorVisualizer by SCROLL_ENABLING_VISUALIZER
    
    guiDsl.onBundleSelect {
        val se = gui.getSlotElement(guiSlot)
        if (se !is SlotElement.InventoryLink)
            return@onBundleSelect // only install scroll support on inventories
        if (se.inventory[se.slot]?.type?.let(Tag.ITEMS_BUNDLES::isTagged) == true)
            return@onBundleSelect // don't scroll gui when scrolling on actual bundle
        
        handleBundleScroll(
            window,
            guiDsl.line,
            guiDsl.maxLine,
            bundleSlot
        )
    }
}

/**
 * Makes the background slots (slots are not assigned to any [SlotElement]) scrollable with the mouse wheel,
 * controlling the [ScrollGui].
 */
context(windowDsl: WindowDsl, guiDsl: ScrollGuiDsl<*>)
fun installBackgroundScrollSupport() {
    val gui by guiDsl.gui
    val window by windowDsl.window
    
    guiDsl.background by guiDsl.background.map {
        SCROLL_ENABLING_VISUALIZER.invoke(it?.get() ?: ItemUtils.getPlaceholder())
    }
    guiDsl.onBundleSelect {
        if (gui.getSlotElement(guiSlot) != null)
            return@onBundleSelect // only background scrolling
        
        handleBundleScroll(
            window,
            guiDsl.line,
            guiDsl.maxLine,
            bundleSlot
        )
    }
}

/**
 * Enables mouse-wheel-scroll handling on the item, scrolling through the [ScrollGui].
 * Requires a scrollable item provider, which can be obtained through [scrollableItemProvider] or by using [SCROLLABLE_BASE] as a base.
 * 
 * Usage:
 * ```
 * item {
 *     itemProvider by itemProvider(SCROLLABLE_BASE) {
 *         name by "Example"
 *     }
 *     installItemScrollSupport()
 * }
 * ```
 */
context(windowDsl: WindowDsl, guiDsl: ScrollGuiDsl<*>, itemDsl: ItemDsl)
fun installItemScrollSupport() {
    itemDsl.onBundleSelect { handleBundleScroll(windowDsl.window.get(), guiDsl.line, guiDsl.maxLine, bundleSlot) }
}

/**
 * Creates an [ItemProvider] that supports mouse-wheel-scrolling via bundle selection
 * (handleable via [Item.handleBundleSelect], [Inventory.bundleSelectHandlers], [Gui.handleBundleSelect])
 * from [itemStack].
 */
fun scrollableItemProvider(itemStack: ItemStack): ItemProvider =
    SCROLL_ENABLING_VISUALIZER_EMPTIES(itemStack) ?: ItemWrapper(itemStack)

/**
 * Creates an [ItemProvider] that supports mouse-wheel-scrolling via bundle selection
 * (handleable via [Item.handleBundleSelect], [Inventory.bundleSelectHandlers], [Gui.handleBundleSelect])
 * from [itemProvider].
 */
fun scrollableItemProvider(itemProvider: ItemProvider): ItemProvider =
    SCROLL_ENABLING_VISUALIZER_EMPTIES(itemProvider.get()) ?: itemProvider
//</editor-fold>

//<editor-fold desc="scroll logic">
private data class ScrollData(val previousWindowState: Int, val previousSlot: Int)

private val trackedScrollData = Collections.synchronizedMap(WeakHashMap<Window, ScrollData>())

private fun getScrollData(window: Window): ScrollData {
    return trackedScrollData.computeIfAbsent(window) { ScrollData(0, -1) }
}

private fun updateScrollData(window: Window, newWindowState: Int, newSlot: Int) {
    trackedScrollData[window] = ScrollData(newWindowState, newSlot)
}

private fun handleBundleScroll(
    window: Window,
    line: MutableProvider<Int>,
    maxLine: Provider<Int>,
    bundleSlot: Int
) {
    val clientWindowState = window.clientWindowState
    var line by line
    val maxLine by maxLine
    
    var (previousWindowState, previousSlot) = getScrollData(window)
    
    if (previousWindowState != clientWindowState) {
        previousSlot = -1
        previousWindowState = clientWindowState
    }
    
    when (determineScrollDirection(previousSlot, bundleSlot)) {
        1 if line > 0 -> {
            line--
            window.incrementWindowState()
        }
        
        -1 if line < maxLine -> {
            line++
            window.incrementWindowState()
        }
        
        else -> Unit
    }
    
    previousSlot = bundleSlot
    
    updateScrollData(window, previousWindowState, previousSlot)
}

/**
 * Returns the scroll direction the scroll wheel turned when moving from the `from` slot to the `to` bundle slot,
 * where `1` means up, `-1` means down and `0` means no or unknown movement.
 */
private fun determineScrollDirection(from: Int, to: Int): Int = when (to) {
    -1 -> 0 // release
    FIRST_SLOT if from == -1 -> -1
    LAST_SLOT if from == -1 -> 1
    (from + 1).mod(SLOTS) -> -1
    (from - 1).mod(SLOTS) -> 1
    else -> 0 // illegal transition
}
//</editor-fold>