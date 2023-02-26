package xyz.xenondevs.nova.ui

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.ScrollGui
import xyz.xenondevs.invui.gui.structure.Markers
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.impl.BaseItem
import xyz.xenondevs.invui.item.impl.SimpleItem
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.nova.material.CoreGuiMaterial
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.config.side.BackItem
import xyz.xenondevs.nova.ui.item.ScrollLeftItem
import xyz.xenondevs.nova.ui.item.ScrollRightItem
import xyz.xenondevs.nova.util.addItemCorrectly
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.util.playItemPickupSound

class UpgradesGui(val upgradeHolder: UpgradeHolder, openPrevious: (Player) -> Unit) {
    
    private val upgradeItems = ArrayList<Item>()
    
    private val upgradeScrollGui = ScrollGui.items()
        .setStructure(
            "x x x x x",
            "x x x x x",
            "< - - - >"
        )
        .addIngredient('<', ScrollLeftItem())
        .addIngredient('>', ScrollRightItem())
        .addIngredient('x', Markers.CONTENT_LIST_SLOT_VERTICAL)
        .setBackground(CoreGuiMaterial.INVENTORY_PART.clientsideProvider)
        .setContent(createUpgradeItemList())
        .build()
    
    val gui: Gui = Gui.normal()
        .setStructure(
            "b - - - - - - - 2",
            "| i # . . . . . |",
            "| # # . . . . . |",
            "3 - - . . . . . 4"
        )
        .addIngredient('i', upgradeHolder.input)
        .addIngredient('b', BackItem(openPrevious))
        .build()
        .apply { fillRectangle(3, 1, upgradeScrollGui, true) }
    
    init {
        upgradeHolder.lazyGui.value.subGuis += gui
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
        Window.single {
            it.setViewer(player)
            it.setTitle(arrayOf(TranslatableComponent("menu.nova.upgrades")))
            it.setGui(gui)
        }.show()
    }
    
    fun updateUpgrades() {
        upgradeItems.forEach(Item::notifyWindows)
    }
    
    private inner class UpgradeDisplay(private val type: UpgradeType<*>) : BaseItem() {
        
        init {
            upgradeItems += this
        }
        
        override fun getItemProvider(): ItemProvider {
            val builder = type.icon.createClientsideItemBuilder()
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
                CoreGuiMaterial.NUMBER.item.createClientsideItemBuilder(subId = upgradeHolder.upgrades[type] ?: 0)
            else CoreGuiMaterial.MINUS.clientsideProvider
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) = Unit
        
    }
    
}

class OpenUpgradesItem(private val upgradeHolder: UpgradeHolder) : SimpleItem(CoreGuiMaterial.UPGRADES_BTN.clientsideProvider) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        player.playClickSound()
        upgradeHolder.gui.openWindow(player)
    }
    
}