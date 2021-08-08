package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterial

open class ChangeNumberItem(
    private val range: IntRange,
    private val sizeModifier: Int,
    private val getNumber: () -> Int,
    private val setNumber: (Int) -> Unit,
    private val onBuilder: ItemBuilder,
    private val offBuilder: ItemBuilder
) : BaseItem() {
    
    override fun getItemProvider(): ItemProvider = if (canModify()) onBuilder else offBuilder
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        if (canModify()) {
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            setNumber(getNumber() + sizeModifier)
        }
    }
    
    private fun canModify() = getNumber() + sizeModifier in range
    
}

class DisplayNumberItem(private val getNumber: () -> Int) : BaseItem() {
    
    override fun getItemProvider(): ItemProvider = NovaMaterial.NUMBER.item.getItemBuilder(getNumber())
    
    override fun handleClick(clickType: ClickType?, player: Player?, event: InventoryClickEvent?) = Unit
    
}

class AddNumberItem(
    range: IntRange,
    getNumber: () -> Int,
    setNumber: (Int) -> Unit
) : ChangeNumberItem(
    range,
    1,
    getNumber,
    setNumber,
    NovaMaterial.PLUS_ON_BUTTON.createBasicItemBuilder(),
    NovaMaterial.PLUS_OFF_BUTTON.createBasicItemBuilder()
)

class RemoveNumberItem(
    range: IntRange,
    getNumber: () -> Int,
    setNumber: (Int) -> Unit,
) : ChangeNumberItem(
    range,
    -1,
    getNumber,
    setNumber,
    NovaMaterial.MINUS_ON_BUTTON.createBasicItemBuilder(),
    NovaMaterial.MINUS_OFF_BUTTON.createBasicItemBuilder()
)