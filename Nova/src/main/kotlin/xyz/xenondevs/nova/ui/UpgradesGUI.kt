package xyz.xenondevs.nova.ui

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.gui.structure.Markers
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
import xyz.xenondevs.nova.material.CoreGUIMaterial
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.config.side.BackItem
import xyz.xenondevs.nova.ui.item.ScrollLeftItem
import xyz.xenondevs.nova.ui.item.ScrollRightItem
import xyz.xenondevs.nova.util.addItemCorrectly
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.playItemPickupSound

class UpgradesGUI(val upgradeHolder: UpgradeHolder, openPrevious: (Player) -> Unit) {
    
    private val upgradeItems = ArrayList<Item>()
    
    private val upgradeScrollGUI = GUIBuilder(GUIType.SCROLL_ITEMS)
        .setStructure(
            "x x x x x",
            "x x x x x",
            "< - - - >"
        )
        .addIngredient('<', ScrollLeftItem())
        .addIngredient('>', ScrollRightItem())
        .addIngredient('x', Markers.ITEM_LIST_SLOT_VERTICAL)
        .setBackground(CoreGUIMaterial.INVENTORY_PART.itemProvider)
        .setItems(createUpgradeItemList())
        .build()
    
    val gui: GUI = GUIBuilder(GUIType.NORMAL)
        .setStructure(
            "b - - - - - - - 2",
            "| i # . . . . . |",
            "| # # . . . . . |",
            "3 - - . . . . . 4"
        )
        .addIngredient('i', upgradeHolder.input)
        .addIngredient('b', BackItem(openPrevious))
        .build()
        .apply { fillRectangle(3, 1, upgradeScrollGUI, true) }
    
    init {
        upgradeHolder.lazyGUI.value.subGUIs += gui
    }
    
    private fun createUpgradeItemList(): List<Item> {
        val list = ArrayList<Item>()
        upgradeHolder.allowed.forEach {
            list += UpgradeDisplay(it)
            list += UpgradeCounter(it)
        }
        return list
    }
    
    fun openWindow(player: Player) {
        SimpleWindow(player, arrayOf(TranslatableComponent("menu.nova.upgrades")), gui).show()
    }
    
    fun updateUpgrades() {
        upgradeItems.forEach(Item::notifyWindows)
    }
    
    private inner class UpgradeDisplay(private val type: UpgradeType<*>) : BaseItem() {
        
        init {
            upgradeItems += this
        }
        
        override fun getItemProvider(): ItemProvider {
            val builder = type.icon.createBasicItemBuilder()
            val typeId = type.id
            builder.setDisplayName(localized(
                ChatColor.GRAY,
                "menu.${typeId.namespace}.upgrades.type.${typeId.name}",
                upgradeHolder.upgrades[type] ?: 0,
                upgradeHolder.getLimit(type)
            ))
            
            return builder
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            val item = upgradeHolder.removeUpgrade(type, clickType.isShiftClick) ?: return
            val location = player.location
            val leftover = player.inventory.addItemCorrectly(item)
            if (leftover != 0) location.world!!.dropItemNaturally(location, item.apply { amount = leftover })
            else player.playItemPickupSound()
        }
        
    }
    
    private inner class UpgradeCounter(private val type: UpgradeType<*>) : BaseItem() {
        
        init {
            upgradeItems += this
        }
        
        override fun getItemProvider(): ItemProvider {
            return if (type in upgradeHolder.allowed)
                CoreGUIMaterial.NUMBER.item.createItemBuilder(upgradeHolder.upgrades[type] ?: 0)
            else CoreGUIMaterial.MINUS.createBasicItemBuilder()
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) = Unit
        
    }
    
}

class OpenUpgradesItem(private val upgradeHolder: UpgradeHolder) : SimpleItem(CoreGUIMaterial.UPGRADES_BTN.itemProvider) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
        upgradeHolder.gui.openWindow(player)
    }
    
}