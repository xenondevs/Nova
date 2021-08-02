package xyz.xenondevs.nova.ui

import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.item.impl.SimpleItem
import de.studiocode.invui.resourcepack.Icon
import de.studiocode.invui.window.impl.single.SimpleWindow
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.ui.config.BackItem
import xyz.xenondevs.nova.upgrade.UpgradeHolder
import xyz.xenondevs.nova.upgrade.UpgradeType
import xyz.xenondevs.nova.util.addItemCorrectly

class UpgradesGUI(val upgradeHolder: UpgradeHolder, openPrevious: (Player) -> Unit) {
    
    private val speedCounter = UpgradeCounter(UpgradeType.SPEED)
    private val efficiencyCounter = UpgradeCounter(UpgradeType.EFFICIENCY)
    private val energyCounter = UpgradeCounter(UpgradeType.ENERGY)
    
    val gui = GUIBuilder(GUIType.NORMAL, 9, 3)
        .setStructure("" +
            "b # # # # # s # #" +
            "# # i # a # f # #" +
            "# # # # # # e # #")
        .addIngredient('b', BackItem(openPrevious))
        .addIngredient('i', VISlotElement(upgradeHolder.input, 0))
        .addIngredient('a', Icon.ARROW_2_RIGHT.item)
        .addIngredient('s', speedCounter)
        .addIngredient('f', efficiencyCounter)
        .addIngredient('e', energyCounter)
        .build()!!
    
    fun openWindow(player: Player) {
        SimpleWindow(player, arrayOf(TranslatableComponent("menu.nova.upgrades")), gui).show()
    }
    
    fun closeForAllViewers() = gui.closeForAllViewers()
    
    fun updateUpgrades() {
        speedCounter.notifyWindows()
        efficiencyCounter.notifyWindows()
        energyCounter.notifyWindows()
    }
    
    // TODO counter with inventory background
    inner class UpgradeCounter(val type: UpgradeType) : BaseItem() {
        
        override fun getItemBuilder(): ItemBuilder {
            return type.material.createBasicItemBuilder()
                .setAmount(upgradeHolder.upgrades[type] ?: 0)
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            val item = upgradeHolder.removeUpgrade(type) ?: return
            player.inventory.addItemCorrectly(item)
        }
        
    }
    
}

class OpenUpgradesItem(val upgradesGUI: UpgradesGUI) : SimpleItem(NovaMaterial.UPGRADES_BUTTON.item.getItemBuilder("menu.nova.upgrades")) {
    
    constructor(upgradeHolder: UpgradeHolder) : this(upgradeHolder.gui)
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
        upgradesGUI.openWindow(player)
    }
    
}