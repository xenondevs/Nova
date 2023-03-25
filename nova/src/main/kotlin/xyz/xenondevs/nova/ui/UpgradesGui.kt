package xyz.xenondevs.nova.ui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.ScrollGui
import xyz.xenondevs.invui.gui.structure.Markers
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.builder.setDisplayName
import xyz.xenondevs.invui.item.impl.AbstractItem
import xyz.xenondevs.invui.item.impl.SimpleItem
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.invui.window.type.context.setTitle
import xyz.xenondevs.nova.material.DefaultGuiMaterial
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.item.BackItem
import xyz.xenondevs.nova.ui.item.ScrollLeftItem
import xyz.xenondevs.nova.ui.item.ScrollRightItem
import xyz.xenondevs.nova.util.addItemCorrectly
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
        .setBackground(DefaultGuiMaterial.INVENTORY_PART.clientsideProvider)
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
    
    private fun createUpgradeItemList(): List<Item> {
        val list = ArrayList<Item>()
        upgradeHolder.allowed.forEach {
            list += UpgradeDisplay(it)
            list += UpgradeCounter(it)
        }
        return list
    }
    
    fun openWindow(player: Player) {
        val window = Window.single {
            it.setViewer(player)
            it.setTitle(Component.translatable("menu.nova.upgrades"))
            it.setGui(gui)
        }
        
        upgradeHolder.menuContainer.registerWindow(window)
        window.open()
    }
    
    fun updateUpgrades() {
        upgradeItems.forEach(Item::notifyWindows)
    }
    
    private inner class UpgradeDisplay(private val type: UpgradeType<*>) : AbstractItem() {
        
        init {
            upgradeItems += this
        }
        
        override fun getItemProvider(): ItemProvider {
            val builder = type.icon.createClientsideItemBuilder()
            val typeId = type.id
            builder.setDisplayName(Component.translatable(
                "menu.${typeId.namespace}.upgrades.type.${typeId.name}",
                NamedTextColor.GRAY,
                Component.text(upgradeHolder.upgrades[type] ?: 0),
                Component.text(upgradeHolder.getLimit(type))
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
    
    private inner class UpgradeCounter(private val type: UpgradeType<*>) : AbstractItem() {
        
        init {
            upgradeItems += this
        }
        
        override fun getItemProvider(): ItemProvider {
            return if (type in upgradeHolder.allowed)
                DefaultGuiMaterial.NUMBER.model.createClientsideItemBuilder(modelId = upgradeHolder.upgrades[type] ?: 0)
            else DefaultGuiMaterial.MINUS.clientsideProvider
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) = Unit
        
    }
    
}

class OpenUpgradesItem(private val upgradeHolder: UpgradeHolder) : SimpleItem(DefaultGuiMaterial.UPGRADES_BTN.clientsideProvider) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        player.playClickSound()
        upgradeHolder.gui.openWindow(player)
    }
    
}