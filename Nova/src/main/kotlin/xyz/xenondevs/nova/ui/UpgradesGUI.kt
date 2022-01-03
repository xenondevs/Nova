package xyz.xenondevs.nova.ui

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.item.impl.SimpleItem
import de.studiocode.invui.window.impl.single.SimpleWindow
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.config.side.BackItem
import xyz.xenondevs.nova.util.addItemCorrectly
import xyz.xenondevs.nova.util.data.localized
import kotlin.random.Random

class UpgradesGUI(val upgradeHolder: UpgradeHolder, openPrevious: (Player) -> Unit) {
    
    private val upgradeItems = ArrayList<Item>()
    
    val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 4)
        .setStructure("" +
            "< - - - - - - - 2" +
            "| i # s c e r f |" +
            "| # # S C E R F |" +
            "3 - - - - - - - 4")
        .addIngredient('<', BackItem(openPrevious))
        .addIngredient('i', VISlotElement(upgradeHolder.input, 0))
        .addIngredient('s', UpgradeDisplay(UpgradeType.SPEED))
        .addIngredient('c', UpgradeDisplay(UpgradeType.EFFICIENCY))
        .addIngredient('e', UpgradeDisplay(UpgradeType.ENERGY))
        .addIngredient('r', UpgradeDisplay(UpgradeType.RANGE))
        .addIngredient('f', UpgradeDisplay(UpgradeType.FLUID))
        .addIngredient('S', UpgradeCounter(UpgradeType.SPEED))
        .addIngredient('C', UpgradeCounter(UpgradeType.EFFICIENCY))
        .addIngredient('E', UpgradeCounter(UpgradeType.ENERGY))
        .addIngredient('R', UpgradeCounter(UpgradeType.RANGE))
        .addIngredient('F', UpgradeCounter(UpgradeType.FLUID))
        .build()
    
    init {
        upgradeHolder.lazyGUI.value.subGUIs += gui
    }
    
    fun openWindow(player: Player) {
        SimpleWindow(player, arrayOf(TranslatableComponent("menu.nova.upgrades")), gui).show()
    }
    
    fun updateUpgrades() {
        upgradeItems.forEach(Item::notifyWindows)
    }
    
    private inner class UpgradeDisplay(private val type: UpgradeType) : BaseItem() {
        
        init {
            upgradeItems += this
        }
        
        override fun getItemProvider(): ItemProvider {
            val builder = (if (type in upgradeHolder.allowed) type.icon else type.grayIcon).createBasicItemBuilder()
            val typeName = type.name.lowercase()
            if (type in upgradeHolder.allowed) {
                builder.setDisplayName(localized(
                    ChatColor.GRAY,
                    "menu.nova.upgrades.type.$typeName",
                    upgradeHolder.upgrades[type] ?: 0,
                    upgradeHolder.getLimit(type)
                ))
            } else builder.setDisplayName(localized(ChatColor.RED, "menu.nova.upgrades.type.$typeName.off"))
            
            return builder
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            val item = upgradeHolder.removeUpgrade(type, clickType.isShiftClick) ?: return
            val location = player.location
            val leftover = player.inventory.addItemCorrectly(item)
            if (leftover != 0) location.world!!.dropItemNaturally(location, item.apply { amount = leftover })
            else player.playSound(location, Sound.ENTITY_ITEM_PICKUP, 0.5f, Random.nextDouble(0.5, 0.7).toFloat())
        }
        
    }
    
    private inner class UpgradeCounter(private val type: UpgradeType) : BaseItem() {
        
        init {
            upgradeItems += this
        }
        
        override fun getItemProvider(): ItemProvider {
            return if (type in upgradeHolder.allowed)
                NovaMaterialRegistry.NUMBER.item.createItemBuilder(upgradeHolder.upgrades[type] ?: 0)
            else NovaMaterialRegistry.NO_NUMBER.createBasicItemBuilder()
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) = Unit
        
    }
    
}

class OpenUpgradesItem(private val upgradeHolder: UpgradeHolder) : SimpleItem(NovaMaterialRegistry.UPGRADES_BUTTON.itemProvider) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
        upgradeHolder.gui.openWindow(player)
    }
    
}