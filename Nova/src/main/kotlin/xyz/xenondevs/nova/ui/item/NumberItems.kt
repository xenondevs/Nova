package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.builder.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.CoreGUIMaterial
import xyz.xenondevs.nova.util.playItemPickupSound

open class ChangeNumberItem(
    private val sizeModifier: Int,
    private val shiftSizeModifier: Int,
    private val getRange: () -> IntRange,
    private val getNumber: () -> Int,
    private val setNumber: (Int) -> Unit,
    private val onBuilder: ItemBuilder,
    private val offBuilder: ItemBuilder
) : BaseItem() {
    
    override fun getItemProvider(): ItemProvider = if (canModify()) onBuilder else offBuilder
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        if (canModify()) {
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
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
    setNumber: (Int) -> Unit
) : ChangeNumberItem(
    1,
    10,
    getRange,
    getNumber,
    setNumber,
    CoreGUIMaterial.PLUS_BTN_ON.createBasicItemBuilder(),
    CoreGUIMaterial.PLUS_BTN_OFF.createBasicItemBuilder()
)

class RemoveNumberItem(
    getRange: () -> IntRange,
    getNumber: () -> Int,
    setNumber: (Int) -> Unit,
) : ChangeNumberItem(
    -1,
    -10,
    getRange,
    getNumber,
    setNumber,
    CoreGUIMaterial.MINUS_BTN_ON.createBasicItemBuilder(),
    CoreGUIMaterial.MINUS_BTN_OFF.createBasicItemBuilder()
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