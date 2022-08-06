package xyz.xenondevs.nova.ui.config.side

import de.studiocode.invui.gui.impl.SimpleGUI
import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.data.world.block.property.Directional
import xyz.xenondevs.nova.material.CoreGUIMaterial
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.data.addLocalizedLoreLines
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.enumMapOf
import xyz.xenondevs.nova.util.yaw

internal abstract class BaseSideConfigGUI(
    val holder: EndPointDataHolder
) : SimpleGUI(9, 3) {
    
    private val configItems = enumMapOf<BlockFace, MutableList<Item>>()
    
    protected abstract fun changeConnectionType(blockFace: BlockFace, forward: Boolean): Boolean
    protected abstract fun getConnectionType(blockFace: BlockFace): NetworkConnectionType
    
    fun registerConfigItem(blockFace: BlockFace, item: Item) {
        configItems.getOrPut(blockFace, ::ArrayList) += item
    }
    
    fun getBlockFace(blockSide: BlockSide): Pair<BlockSide?, BlockFace> {
        val directional = (holder.endPoint as TileEntity).blockState.getProperty(Directional::class)
        return if (directional != null)
            blockSide to blockSide.getBlockFace(directional.facing.yaw)
        else null to blockSide.blockFace
    }
    
    fun getSideName(blockSide: BlockSide?, blockFace: BlockFace): TranslatableComponent {
        return if (blockSide != null) {
            localized(
                ChatColor.GRAY,
                "menu.nova.side_config.direction",
                TranslatableComponent("menu.nova.side_config.${blockSide.name.lowercase()}"),
                TranslatableComponent("menu.nova.side_config.${blockFace.name.lowercase()}")
            )
        } else {
            localized(ChatColor.GRAY, "menu.nova.side_config.${blockFace.name.lowercase()}")
        }
    }
    
    fun updateConfigItems(blockFace: BlockFace) {
        configItems[blockFace]?.forEach(Item::notifyWindows)
    }
    
    inner class ConnectionConfigItem(blockSide: BlockSide) : ConfigItem(blockSide) {
        
        override fun getItemProvider(): ItemProvider {
            return when (getConnectionType(blockFace)) {
                NetworkConnectionType.NONE ->
                    CoreGUIMaterial.GRAY_BTN.createClientsideItemBuilder().addLocalizedLoreLines(ChatColor.GRAY, "menu.nova.side_config.none")
                NetworkConnectionType.EXTRACT ->
                    CoreGUIMaterial.ORANGE_BTN.createClientsideItemBuilder().addLocalizedLoreLines(ChatColor.GOLD, "menu.nova.side_config.output")
                NetworkConnectionType.INSERT ->
                    CoreGUIMaterial.BLUE_BTN.createClientsideItemBuilder().addLocalizedLoreLines(ChatColor.AQUA, "menu.nova.side_config.input")
                NetworkConnectionType.BUFFER ->
                    CoreGUIMaterial.GREEN_BTN.createClientsideItemBuilder().addLocalizedLoreLines(ChatColor.GREEN, "menu.nova.side_config.input_output")
            }.setDisplayName(getSideName(blockSide, blockFace))
        }
    
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            if (changeConnectionType(blockFace, clickType.isLeftClick)) {
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                updateConfigItems(blockFace)
            }
        }
        
    }
    
    abstract inner class ConfigItem(blockSide: BlockSide) : BaseItem() {
        
        protected val blockSide: BlockSide?
        protected val blockFace: BlockFace
        
        init {
            val pair = getBlockFace(blockSide)
            this.blockSide = pair.first
            this.blockFace = pair.second
            
            registerConfigItem(blockFace, this)
        }
        
    }
    
}