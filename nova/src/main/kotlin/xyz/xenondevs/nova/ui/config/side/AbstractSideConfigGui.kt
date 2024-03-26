@file:Suppress("LeakingThis")

package xyz.xenondevs.nova.ui.config.side

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.invui.gui.AbstractGui
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.builder.addLoreLines
import xyz.xenondevs.invui.item.builder.setDisplayName
import xyz.xenondevs.invui.item.impl.AbstractItem
import xyz.xenondevs.nova.item.DefaultGuiItems
import xyz.xenondevs.nova.tileentity.network.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.playClickSound

internal abstract class AbstractSideConfigGui<H : EndPointDataHolder>(
    val holder: H
) : AbstractGui(9, 3) {
    
    private val configItems = enumMap<BlockFace, MutableList<Item>>()
    
    fun registerConfigItem(blockFace: BlockFace, item: Item) {
        configItems.getOrPut(blockFace, ::ArrayList) += item
    }
    
    fun getBlockFace(blockSide: BlockSide): Pair<BlockSide?, BlockFace> {
        TODO()
//        val directional = (holder.endPoint as TileEntity).blockState.getProperty(Directional::class)
//        return if (directional != null)
//            blockSide to blockSide.getBlockFace(directional.facing.yaw)
//        else null to blockSide.blockFace
    }
    
    fun getSideName(blockSide: BlockSide?, blockFace: BlockFace): Component {
        return if (blockSide != null) {
            Component.text()
                .color(NamedTextColor.GRAY)
                .append(Component.translatable("menu.nova.side_config.${blockSide.name.lowercase()}"))
                .append(Component.text(" ("))
                .append(Component.translatable("menu.nova.side_config.${blockFace.name.lowercase()}"))
                .append(Component.text(")"))
                .build()
        } else {
            Component.translatable("menu.nova.side_config.${blockFace.name.lowercase()}", NamedTextColor.GRAY)
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
                    DefaultGuiItems.GRAY_BTN.model.createClientsideItemBuilder()
                        .addLoreLines(Component.translatable("menu.nova.side_config.none", NamedTextColor.GRAY))
                NetworkConnectionType.EXTRACT ->
                    DefaultGuiItems.ORANGE_BTN.model.createClientsideItemBuilder()
                        .addLoreLines(Component.translatable( "menu.nova.side_config.output", NamedTextColor.GOLD))
                NetworkConnectionType.INSERT ->
                    DefaultGuiItems.BLUE_BTN.model.createClientsideItemBuilder()
                        .addLoreLines(Component.translatable("menu.nova.side_config.input", NamedTextColor.AQUA))
                NetworkConnectionType.BUFFER ->
                    DefaultGuiItems.GREEN_BTN.model.createClientsideItemBuilder()
                        .addLoreLines(Component.translatable("menu.nova.side_config.input_output", NamedTextColor.GREEN))
            }.setDisplayName(getSideName(blockSide, blockFace))
        }
    
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            if (changeConnectionType(blockFace, clickType.isLeftClick)) {
                player.playClickSound()
                updateConfigItems(blockFace)
            }
        }
        
    }
    
    abstract inner class ConfigItem(blockSide: BlockSide) : AbstractItem() {
        
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