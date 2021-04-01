package xyz.xenondevs.nova.ui

import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.resourcepack.Icon
import de.studiocode.invui.virtualinventory.VirtualInventoryManager
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import de.studiocode.invui.window.impl.single.SimpleWindow
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.ui.item.ProgressArrowItem
import xyz.xenondevs.nova.util.runTaskTimer
import java.util.*

class TestUI(player: Player, uuid: UUID) : Listener {
    
    private val input = VirtualInventoryManager.getInstance().getOrCreate(UUID.nameUUIDFromBytes("$uuid-in".toByteArray()), 1)
    private val output = VirtualInventoryManager.getInstance().getOrCreate(UUID.nameUUIDFromBytes("$uuid-out".toByteArray()), 1)
    
    private val progress = ProgressArrowItem()
    
    private val gui = GUIBuilder(GUIType.NORMAL, 9, 3)
        .setStructure("" +
            "# # # # # # # # #" +
            "# # i # > # o # #" +
            "# # # # # # # # #")
        .addIngredient('#', Icon.BACKGROUND.item)
        .addIngredient('i', VISlotElement(input, 0))
        .addIngredient('o', VISlotElement(output, 0))
        .addIngredient('>', progress)
        .build()
    
    init {
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
        runTaskTimer(0, 10) {
            if (progress.state + 1 > 16) progress.state = 0
            else progress.state++
        }
        
        SimpleWindow(player, "Furnace UI", gui).show()
    }
    
    @EventHandler
    fun handleVIUpdate(event: ItemUpdateEvent) {
        val inventory = event.virtualInventory
        if (inventory == input) {
            if (event.newItemStack != null && event.newItemStack?.type != Material.IRON_ORE)
                event.isCancelled = true
        } else if (inventory == output && event.player != null && event.newItemStack != null)
            event.isCancelled = true
    }
    
}