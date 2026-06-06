package xyz.xenondevs.nova.ui.menu.item

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.CustomModelData.customModelData
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.dsl.ClickDsl
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.item.AbstractItem
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.ui.menu.itemProvider
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.util.playItemPickupSound
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.clientsideProvider

/**
 * A UI item for changing [number] by the following values:
 * - [leftClick] on left-click
 * - [shiftLeftClick] on shift-left-click
 * - [rightClick] on right-click
 * - [shiftRightClick] on shift-right-click
 * 
 * The number is coerced in [range]. If the number is already at the limit of the range, [offProvider] is used,
 * otherwise [onProvider] is used.
 */
fun changeNumberItem(
    leftClick: Int = 1,
    shiftLeftClick: Int = 10,
    rightClick: Int = -1,
    shiftRightClick: Int = -10,
    range: Provider<IntRange>,
    number: MutableProvider<Int>,
    onProvider: Provider<ItemProvider>,
    offProvider: Provider<ItemProvider>
) = item {
    itemProvider by combinedProvider(
        number, range, onProvider, offProvider
    ) { number, range, onProvider, offProvider ->
        when {
            (number + leftClick).coerceIn(range) != number -> onProvider
            (number + shiftLeftClick).coerceIn(range) != number -> onProvider
            (number + rightClick).coerceIn(range) != number -> onProvider
            (number + shiftRightClick).coerceIn(range) != number -> onProvider
            else -> offProvider
        }
    }.map { scrollableItemProvider(it) }
    
    var number by number
    val range by range
    
    onClick {
        val targetNumber = (number + when(clickType) {
            ClickType.LEFT -> leftClick
            ClickType.SHIFT_LEFT -> shiftLeftClick
            ClickType.RIGHT -> rightClick
            ClickType.SHIFT_RIGHT -> shiftRightClick
            else -> 0
        }).coerceIn(range)
        
        if (targetNumber != number) {
            number = targetNumber
            player.playClickSound()
        }
    }
}

/**
 * A UI item for changing [number] by 1 on click and 10 of shift-click.
 * The number is coerced in range. If the number is already at the limit of the range,
 * [DefaultGuiItems.TP_PLUS_BTN_OFF] is used, otherwise [DefaultGuiItems.TP_PLUS_BTN_ON] is used.
 * Optionally, the ON variant has a tooltip using the translation key [localizedName].
 */
fun addNumberItem(
    range: Provider<IntRange>,
    number: MutableProvider<Int>,
    localizedName: String? = null,
) = changeNumberItem(
    1, 10, 0, 0,
    range,
    number,
    localizedName
        ?.let { ln -> itemProvider(DefaultGuiItems.TP_PLUS_BTN_ON) { name by Component.translatable(ln) } }
        ?: DefaultGuiItems.TP_PLUS_BTN_ON.clientsideProvider,
    DefaultGuiItems.TP_PLUS_BTN_OFF.clientsideProvider
)

/**
 * A UI item for changing [number] by -1 on click and -10 of shift-click.
 * The number is coerced in range. If the number is already at the limit of the range,
 * [DefaultGuiItems.TP_MINUS_BTN_OFF] is used, otherwise [DefaultGuiItems.TP_MINUS_BTN_ON] is used.
 * Optionally, the ON variant has a tooltip using the translation key [localizedName].
 */
fun removeNumberItem(
    range: Provider<IntRange>,
    number: MutableProvider<Int>,
    localizedName: String? = null,
) = changeNumberItem(
    -1, -10, 0, 0,
    range,
    number,
    localizedName
        ?.let { ln -> itemProvider(DefaultGuiItems.TP_MINUS_BTN_ON) { name by Component.translatable(ln) } }
        ?: DefaultGuiItems.TP_MINUS_BTN_ON.clientsideProvider,
    DefaultGuiItems.TP_MINUS_BTN_OFF.clientsideProvider
)

/**
 * A UI item that displays [number] via [DefaultGuiItems.TP_NUMBER].
 * Optionally, the item has a tooltip using the translation key [localizedName].
 * Optionally, [onClick] defines a click action.
 */
fun displayNumberItem(
    number: Provider<Int>,
    localizedName: String? = null,
    onClick: ClickDsl.() -> Unit = {}
) = item {
    itemProvider by itemProvider(DefaultGuiItems.TP_NUMBER) {
        if (localizedName != null) {
            name by number.map { Component.translatable(localizedName, Component.text(it)) }
        }
        data[DataComponentTypes.CUSTOM_MODEL_DATA] by number.map {
            customModelData()
                .addFloat(it.toFloat())
                .build()
        }
    }
    onClick(onClick)
}

open class ChangeNumberItem(
    private val sizeModifier: Int,
    private val shiftSizeModifier: Int,
    private val getRange: () -> IntRange,
    private val getNumber: () -> Int,
    private val setNumber: (Int) -> Unit,
    private val onProvider: Provider<ItemProvider>,
    private val offProvider: Provider<ItemProvider>
) : AbstractItem() {
    
    constructor(
        sizeModifier: Int,
        shiftSizeModifier: Int,
        getRange: () -> IntRange,
        getNumber: () -> Int,
        setNumber: (Int) -> Unit,
        onProvider: ItemProvider,
        offProvider: ItemProvider
    ) : this(sizeModifier, shiftSizeModifier, getRange, getNumber, setNumber, provider(onProvider), provider(offProvider))
    
    init {
        onProvider.observeWeak(this) { thisRef -> thisRef.notifyWindows() }
        offProvider.observeWeak(this) { thisRef -> thisRef.notifyWindows() }
    }
    
    override fun getItemProvider(player: Player): ItemProvider = if (canModify()) onProvider.get() else offProvider.get()
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        if (canModify()) {
            player.playClickSound()
            setNumber((getNumber() + if (clickType.isShiftClick) shiftSizeModifier else sizeModifier).coerceIn(getRange()))
        }
    }
    
    private fun canModify() = getNumber() + sizeModifier in getRange()
    
}

class DisplayNumberItem(private val getNumber: () -> Int, private val localizedName: String? = null) : AbstractItem() {
    
    constructor(getNumber: () -> Int) : this(getNumber, null)
    
    init {
        DefaultGuiItems.NUMBER.observeWeak(this) { thisRef -> thisRef.notifyWindows() }
    }
    
    override fun getItemProvider(player: Player): ItemProvider {
        val number = getNumber().coerceIn(0..999)
        val builder = DefaultGuiItems.NUMBER.get()
            .createClientsideItemBuilder()
            .addCustomModelData(number)
        if (localizedName != null)
            builder.setName(Component.translatable(localizedName, Component.text(number)))
        return builder
    }
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) = Unit
    
}

@Deprecated(LEGACY_NON_DSL_INVUI_DEPRECATION)
class AddNumberItem(
    getRange: () -> IntRange,
    getNumber: () -> Int,
    setNumber: (Int) -> Unit,
    localizedName: String? = null
) : ChangeNumberItem(
    1,
    10,
    getRange,
    getNumber,
    setNumber,
    localizedName
        ?.let { ln -> itemProvider(DefaultGuiItems.PLUS_BTN_ON) { name by Component.translatable(ln) } }
        ?: DefaultGuiItems.PLUS_BTN_ON.clientsideProvider,
    DefaultGuiItems.PLUS_BTN_OFF.clientsideProvider
)

class RemoveNumberItem(
    getRange: () -> IntRange,
    getNumber: () -> Int,
    setNumber: (Int) -> Unit,
    localizedName: String? = null
) : ChangeNumberItem(
    -1,
    -10,
    getRange,
    getNumber,
    setNumber,
    localizedName
        ?.let { ln -> itemProvider(DefaultGuiItems.MINUS_BTN_ON) { name by Component.translatable(ln) } }
        ?: DefaultGuiItems.MINUS_BTN_ON.clientsideProvider,
    DefaultGuiItems.MINUS_BTN_OFF.clientsideProvider
)

open class AioNumberItem(
    private val numberModifier: Int,
    private val shiftNumberModifier: Int,
    private val getRange: () -> IntRange,
    private val getNumber: () -> Int,
    private val setNumber: (Int) -> Unit,
    private val localizedName: String,
    private val builder: ItemBuilder
) : AbstractItem() {
    
    override fun getItemProvider(player: Player): ItemProvider =
        builder.setName(Component.translatable(localizedName, Component.text(getNumber())))
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        val numberModifier = when (clickType) {
            ClickType.LEFT -> numberModifier
            ClickType.SHIFT_LEFT -> shiftNumberModifier
            ClickType.RIGHT -> -numberModifier
            ClickType.SHIFT_RIGHT -> -shiftNumberModifier
            else -> return
        }
        
        val currentNumber = getNumber()
        val number = (currentNumber + numberModifier).coerceIn(getRange())
        
        if (number != currentNumber) {
            player.playItemPickupSound()
            setNumber(number)
            notifyWindows()
        }
    }
    
}