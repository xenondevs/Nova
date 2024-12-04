package xyz.xenondevs.nova.ui.menu.item

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.invui.item.AbstractItem
import xyz.xenondevs.invui.item.Click
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.util.playItemPickupSound
import xyz.xenondevs.nova.world.item.DefaultGuiItems

open class ChangeNumberItem(
    private val sizeModifier: Int,
    private val shiftSizeModifier: Int,
    private val getRange: () -> IntRange,
    private val getNumber: () -> Int,
    private val setNumber: (Int) -> Unit,
    private val onProvider: ItemProvider,
    private val offProvider: ItemProvider
) : AbstractItem() {
    
    override fun getItemProvider(player: Player): ItemProvider = if (canModify()) onProvider else offProvider
    
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
    
    override fun getItemProvider(player: Player): ItemProvider {
        val number = getNumber().coerceIn(0..999)
        val builder = DefaultGuiItems.NUMBER
            .createClientsideItemBuilder()
            .addCustomModelData(number)
        if (localizedName != null)
            builder.setName(Component.translatable(localizedName, Component.text(number)))
        return builder
    }
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) = Unit
    
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
        ?.let { DefaultGuiItems.PLUS_BTN_ON.createClientsideItemBuilder().setName(Component.translatable(it)) }
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
        ?.let { DefaultGuiItems.MINUS_BTN_ON.createClientsideItemBuilder().setName(Component.translatable(it)) }
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