package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.builder.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.CoreGUIMaterial
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
) : BaseItem() {
    
    override fun getItemProvider(): ItemProvider = if (canModify()) onProvider else offProvider
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        if (canModify()) {
            player.playClickSound()
            setNumber((getNumber() + if (clickType.isShiftClick) shiftSizeModifier else sizeModifier).coerceIn(getRange()))
        }
    }
    
    private fun canModify() = getNumber() + sizeModifier in getRange()
    
}

class DisplayNumberItem(private val getNumber: () -> Int, private val localizedName: String? = null) : BaseItem() {
    
    constructor(getNumber: () -> Int) : this(getNumber, null)
    
    override fun getItemProvider(): ItemProvider {
        val number = getNumber().coerceIn(0..999)
        val builder = CoreGUIMaterial.NUMBER.item.createItemBuilder(number)
        if (localizedName != null)
            builder.setDisplayName(TranslatableComponent(localizedName, number))
        
        return builder
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
        ?.let { CoreGUIMaterial.PLUS_BTN_ON.createClientsideItemBuilder().setDisplayName(TranslatableComponent(it)) }
        ?: CoreGUIMaterial.PLUS_BTN_ON.clientsideProvider,
    CoreGUIMaterial.PLUS_BTN_OFF.clientsideProvider
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
        ?.let { CoreGUIMaterial.MINUS_BTN_ON.createClientsideItemBuilder().setDisplayName(TranslatableComponent(it)) } 
        ?: CoreGUIMaterial.MINUS_BTN_ON.clientsideProvider,
    CoreGUIMaterial.MINUS_BTN_OFF.clientsideProvider
)

open class AioNumberItem(
    private val numberModifier: Int,
    private val shiftNumberModifier: Int,
    private val getRange: () -> IntRange,
    private val getNumber: () -> Int,
    private val setNumber: (Int) -> Unit,
    private val localizedName: String,
    private val builder: ItemBuilder
) : BaseItem() {
    
    override fun getItemProvider(): ItemProvider =
        builder.setDisplayName(TranslatableComponent(localizedName, getNumber()))
    
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