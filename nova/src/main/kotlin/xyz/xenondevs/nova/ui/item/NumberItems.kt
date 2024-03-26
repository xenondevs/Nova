package xyz.xenondevs.nova.ui.item

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.item.builder.setDisplayName
import xyz.xenondevs.invui.item.impl.AbstractItem
import xyz.xenondevs.nova.item.DefaultGuiItems
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.util.playItemPickupSound

open class ChangeNumberItem(
    private val sizeModifier: Int,
    private val shiftSizeModifier: Int,
    private val getRange: () -> IntRange,
    private val getNumber: () -> Int,
    private val setNumber: (Int) -> Unit,
    private val onProvider: ItemProvider,
    private val offProvider: ItemProvider
) : AbstractItem() {
    
    override fun getItemProvider(): ItemProvider = if (canModify()) onProvider else offProvider
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        if (canModify()) {
            player.playClickSound()
            setNumber((getNumber() + if (clickType.isShiftClick) shiftSizeModifier else sizeModifier).coerceIn(getRange()))
        }
    }
    
    private fun canModify() = getNumber() + sizeModifier in getRange()
    
}

class DisplayNumberItem(private val getNumber: () -> Int, private val localizedName: String? = null) : AbstractItem() {
    
    constructor(getNumber: () -> Int) : this(getNumber, null)
    
    override fun getItemProvider(): ItemProvider {
        val number = getNumber().coerceIn(0..999)
        return if (localizedName != null) {
            DefaultGuiItems.NUMBER.model.createClientsideItemBuilder(modelId = number)
                .setDisplayName(Component.translatable(localizedName, Component.text(number)))
        } else DefaultGuiItems.NUMBER.model.unnamedClientsideProviders[number]
    }
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) = Unit
    
}

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
        ?.let { DefaultGuiItems.PLUS_BTN_ON.model.createClientsideItemBuilder().setDisplayName(Component.translatable(it)) }
        ?: DefaultGuiItems.PLUS_BTN_ON.model.clientsideProvider,
    DefaultGuiItems.PLUS_BTN_OFF.model.clientsideProvider
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
        ?.let { DefaultGuiItems.MINUS_BTN_ON.model.createClientsideItemBuilder().setDisplayName(Component.translatable(it)) }
        ?: DefaultGuiItems.MINUS_BTN_ON.model.clientsideProvider,
    DefaultGuiItems.MINUS_BTN_OFF.model.clientsideProvider
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
    
    override fun getItemProvider(): ItemProvider =
        builder.setDisplayName(Component.translatable(localizedName, Component.text(getNumber())))
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
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