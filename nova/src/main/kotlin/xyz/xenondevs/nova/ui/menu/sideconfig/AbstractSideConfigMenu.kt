@file:Suppress("LeakingThis")

package xyz.xenondevs.nova.ui.menu.sideconfig

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.commons.collections.after
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.item.builder.addLoreLines
import xyz.xenondevs.invui.item.builder.setDisplayName
import xyz.xenondevs.invui.item.notifyWindows
import xyz.xenondevs.nova.item.DefaultGuiItems
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.node.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.ui.menu.item.AsyncItem
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties

internal abstract class AbstractSideConfigMenu<H : EndPointDataHolder>(
    val endPoint: NetworkEndPoint,
    val networkType: NetworkType<*>,
    val holder: H
) {
    
    val gui = Gui.empty(9, 3)
    
    private val configItems = ArrayList<ConfigItem>()
    protected val connectionConfigItems = enumMap<BlockFace, ArrayList<ConnectionConfigItem>>()
    
    open fun initAsync() {
        configItems.forEach { it.updateAsync() }
        runTask { configItems.forEach { it.notifyWindows() } }
    }
    
    fun getFaceFromSide(blockSide: BlockSide): Pair<BlockSide?, BlockFace> {
        val facing = (endPoint as? TileEntity)?.blockState?.get(DefaultBlockStateProperties.FACING)
        return if (facing != null)
            blockSide to blockSide.getBlockFace(facing)
        else null to blockSide.getBlockFace(0f)
    }
    
    fun getSideName(blockSide: BlockSide?, blockFace: BlockFace): Component {
        if (blockSide != null) {
            return Component.text()
                .color(NamedTextColor.GRAY)
                .append(Component.translatable("menu.nova.side_config.${blockSide.name.lowercase()}"))
                .append(Component.text(" ("))
                .append(Component.translatable("menu.nova.side_config.${blockFace.name.lowercase()}"))
                .append(Component.text(")"))
                .build()
        } else {
            return Component.translatable("menu.nova.side_config.${blockFace.name.lowercase()}", NamedTextColor.GRAY)
        }
    }
    
    private fun queueCycleConnectionType(face: BlockFace, move: Int) {
        NetworkManager.queueWrite(endPoint.pos.world) { state ->
            // cycle connection type
            val allowedTypes = getAllowedConnectionType(face).supertypes
            val currentType = getConnectionType(face)
            setConnectionType(face, allowedTypes.after(currentType, move))
            
            state.getNetwork(endPoint, networkType, face)?.markDirty()
            state.handleEndPointAllowedFacesChange(endPoint, networkType, face)
            
            // ui update
            connectionConfigItems[face]?.forEach(AsyncItem::updateAsync)
            runTask { connectionConfigItems[face]?.notifyWindows() }
        }
    }
    
    protected abstract fun getAllowedConnectionType(face: BlockFace): NetworkConnectionType
    
    protected abstract fun getConnectionType(face: BlockFace): NetworkConnectionType
    
    protected abstract fun setConnectionType(face: BlockFace, type: NetworkConnectionType)
    
    inner class ConnectionConfigItem(blockSide: BlockSide) : ConfigItem(blockSide) {
        
        init {
            connectionConfigItems.getOrPut(face, ::ArrayList) += this
        }
        
        override fun updateAsync() {
            val connectionType = getConnectionType(face)
            provider.set(
                when (connectionType) {
                    NetworkConnectionType.NONE ->
                        DefaultGuiItems.GRAY_BTN.model.createClientsideItemBuilder()
                            .addLoreLines(Component.translatable("menu.nova.side_config.none", NamedTextColor.GRAY))
                    
                    NetworkConnectionType.EXTRACT ->
                        DefaultGuiItems.ORANGE_BTN.model.createClientsideItemBuilder()
                            .addLoreLines(Component.translatable("menu.nova.side_config.output", NamedTextColor.GOLD))
                    
                    NetworkConnectionType.INSERT ->
                        DefaultGuiItems.BLUE_BTN.model.createClientsideItemBuilder()
                            .addLoreLines(Component.translatable("menu.nova.side_config.input", NamedTextColor.AQUA))
                    
                    NetworkConnectionType.BUFFER ->
                        DefaultGuiItems.GREEN_BTN.model.createClientsideItemBuilder()
                            .addLoreLines(Component.translatable("menu.nova.side_config.input_output", NamedTextColor.GREEN))
                }.setDisplayName(getSideName(blockSide, face))
            )
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            player.playClickSound()
            queueCycleConnectionType(face, if (clickType.isLeftClick) 1 else -1)
        }
        
    }
    
    abstract inner class ConfigItem(blockSide: BlockSide) : AsyncItem() {
        
        protected val blockSide: BlockSide?
        protected val face: BlockFace
        
        init {
            val pair = getFaceFromSide(blockSide)
            this.blockSide = pair.first
            this.face = pair.second
            configItems += this
        }
        
    }
    
}