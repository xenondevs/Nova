package xyz.xenondevs.nova.ui.config.cable

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.window.impl.single.SimpleWindow
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.network.fluid.holder.FluidHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.ui.item.ClickyTabItem

class CableConfigGUI(
    val itemHolder: ItemHolder?,
    val fluidHolder: FluidHolder?,
    private val face: BlockFace
) {
    
    private val gui: GUI
    
    private val itemConfigGUI = itemHolder?.let { ItemCableConfigGUI(it, face) }
    private val fluidConfigGUI = fluidHolder?.let { FluidCableConfigGUI(it, face) }
    
    init {
        require(itemConfigGUI != null || fluidConfigGUI != null)
        
        gui = GUIBuilder(GUIType.TAB, 9, 5)
            .setStructure("" +
                "# # # i # f # # #" +
                "- - - - - - - - -" +
                "x x x x x x x x x" +
                "x x x x x x x x x" +
                "x x x x x x x x x")
            .addIngredient('i', ClickyTabItem(0) {
                (if (itemConfigGUI != null) {
                    if (it.currentTab == 0)
                        NovaMaterialRegistry.ITEM_SELECTED_BUTTON
                    else NovaMaterialRegistry.ITEM_ON_BUTTON
                } else NovaMaterialRegistry.ITEM_OFF_BUTTON).itemProvider
            })
            .addIngredient('f', ClickyTabItem(1) {
                (if (fluidConfigGUI != null) {
                    if (it.currentTab == 1)
                        NovaMaterialRegistry.FLUID_SELECTED_BUTTON
                    else NovaMaterialRegistry.FLUID_ON_BUTTON
                } else NovaMaterialRegistry.FLUID_OFF_BUTTON).itemProvider
            })
            .setGUIs(listOf(itemConfigGUI?.gui, fluidConfigGUI?.gui))
            .build()
    }
    
    fun openWindow(player: Player) {
        SimpleWindow(player, arrayOf(TranslatableComponent("menu.nova.cable_config")), gui)
            .also { it.addCloseHandler(::writeChanges) }
            .show()
    }
    
    fun closeForAllViewers() {
        gui.closeForAllViewers()
    }
    
    fun updateValues(updateButtons: Boolean = true) {
        itemConfigGUI?.updateValues(updateButtons)
        fluidConfigGUI?.updateValues(updateButtons)
    }
    
    private fun writeChanges() {
        itemConfigGUI?.writeChanges()
        fluidConfigGUI?.writeChanges()
    }
    
}