package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterialRegistry

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
        val number = getNumber()
        val builder = NovaMaterialRegistry.NUMBER.item.createItemBuilder(number)
        if (localizedName != null)
            builder.setDisplayName(TranslatableComponent(localizedName, number))
        
        return builder
    }
    
    override fun handleClick(clickType: ClickType?, player: Player?, event: InventoryClickEvent?) = Unit
    
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
    NovaMaterialRegistry.PLUS_ON_BUTTON.createBasicItemBuilder(),
    NovaMaterialRegistry.PLUS_OFF_BUTTON.createBasicItemBuilder()
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
    NovaMaterialRegistry.MINUS_ON_BUTTON.createBasicItemBuilder(),
    NovaMaterialRegistry.MINUS_OFF_BUTTON.createBasicItemBuilder()
)