package xyz.xenondevs.nova.ui.config.side

import de.studiocode.invui.gui.impl.SimpleGUI
import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.data.world.block.property.Directional
import xyz.xenondevs.nova.material.CoreGUIMaterial
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.data.addLocalizedLoreLines
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.util.yaw

internal abstract class BaseSideConfigGUI<H : EndPointDataHolder>(
    val holder: H
) : SimpleGUI(9, 3) {
    
    private val configItems = enumMap<BlockFace, MutableList<Item>>()
    
    fun registerConfigItem(blockFace: BlockFace, item: Item) {
        configItems.getOrPut(blockFace, ::ArrayList) += item
    }
    
    fun getBlockFace(blockSide: BlockSide): Pair<BlockSide?, BlockFace> {
        val directional = (holder.endPoint as TileEntity).blockState.getProperty(Directional::class)
        return if (directional != null)
            blockSide to blockSide.getBlockFace(directional.facing.yaw)
        else null to blockSide.blockFace
    }
    
    fun getSideName(blockSide: BlockSide?, blockFace: BlockFace): Array<BaseComponent> {
        return if (blockSide != null) {
            ComponentBuilder()
                .color(ChatColor.GRAY)
                .append(TranslatableComponent("menu.nova.side_config.${blockSide.name.lowercase()}"))
                .append(" (")
                .append(TranslatableComponent("menu.nova.side_config.${blockFace.name.lowercase()}"))
                .append(")")
                .create()
        } else {
            arrayOf(localized(ChatColor.GRAY, "menu.nova.side_config.${blockFace.name.lowercase()}"))
        }
    }
    
    fun updateConfigItems(blockFace: BlockFace) {
        configItems[blockFace]?.forEach(Item::notifyWindows)
    }
    
    private fun changeConnectionType(blockFace: BlockFace, forward: Boolean): Boolean {
        NetworkManager.execute { // TODO: runSync / runAsync ?
            it.removeEndPoint(holder.endPoint, false)
            
            val allowedTypes = getAllowedConnectionTypes(blockFace)
            val currentType = holder.connectionConfig[blockFace]!!
            var index = allowedTypes.indexOf(currentType)
            index = (index + if (forward) 1 else -1).mod(allowedTypes.size)
            holder.connectionConfig[blockFace] = allowedTypes[index]
            
            it.addEndPoint(holder.endPoint, false)
                .thenRun { holder.endPoint.updateNearbyBridges() }
        }
        
        return true
    }
    
    protected abstract fun getAllowedConnectionTypes(blockFace: BlockFace): List<NetworkConnectionType>
    
    inner class ConnectionConfigItem(blockSide: BlockSide) : ConfigItem(blockSide) {
        
        override fun getItemProvider(): ItemProvider {
            val connectionType = holder.connectionConfig[blockFace]!! // fixme: Unsafe network value access. Should only be accessed from NetworkManager thread.
            return when (connectionType) {
                NetworkConnectionType.NONE ->
                    CoreGUIMaterial.GRAY_BTN.createClientsideItemBuilder().addLocalizedLoreLines(ChatColor.GRAY, "menu.nova.side_config.none")
                NetworkConnectionType.EXTRACT ->
                    CoreGUIMaterial.ORANGE_BTN.createClientsideItemBuilder().addLocalizedLoreLines(ChatColor.GOLD, "menu.nova.side_config.output")
                NetworkConnectionType.INSERT ->
                    CoreGUIMaterial.BLUE_BTN.createClientsideItemBuilder().addLocalizedLoreLines(ChatColor.AQUA, "menu.nova.side_config.input")
                NetworkConnectionType.BUFFER ->
                    CoreGUIMaterial.GREEN_BTN.createClientsideItemBuilder().addLocalizedLoreLines(ChatColor.GREEN, "menu.nova.side_config.input_output")
            }.setDisplayName(*getSideName(blockSide, blockFace))
        }
    
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            if (changeConnectionType(blockFace, clickType.isLeftClick)) {
                player.playClickSound()
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