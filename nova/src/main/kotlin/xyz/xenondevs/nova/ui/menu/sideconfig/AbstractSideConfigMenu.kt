@file:Suppress("LeakingThis")

package xyz.xenondevs.nova.ui.menu.sideconfig

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.collections.after
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.flatten
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.ui.menu.itemProvider
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkManager
import xyz.xenondevs.nova.world.block.tileentity.network.node.EndPointDataHolder
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.world.format.NetworkState
import xyz.xenondevs.nova.world.item.DefaultGuiItems

abstract class AbstractSideConfigMenu<H : EndPointDataHolder> internal constructor(
    protected val endPoint: NetworkEndPoint,
    networkType: RegistryEntry.Nova<NetworkType<*>>,
    protected val holder: H
) {
    
    protected val connectionTypes: Map<BlockFace, MutableProvider<NetworkConnectionType>> =
        CUBE_FACES.associateWith { mutableProvider(NetworkConnectionType.NONE) }
    
    protected val networkType by networkType
    
    abstract val gui: Gui
    
    open fun init(state: NetworkState) {
        refresh(state)
    }
    
    open fun refresh(state: NetworkState) {
        connectionTypes.forEach { (face, type) -> type.set(getConnectionType(face)) }
    }
    
    private fun queueCycleConnectionType(face: BlockFace, move: Int) {
        NetworkManager.queueWrite(endPoint.pos.chunkPos) { state ->
            // cycle connection type
            val allowedTypes = getAllowedConnectionType(face).supertypes
            val currentType = getConnectionType(face)
            setConnectionType(face, allowedTypes.after(currentType, move))
            
            state.getNetwork(endPoint, networkType, face)?.markDirty()
            state.handleEndPointAllowedFacesChange(endPoint, networkType, face)
            
            // ui update
            refresh(state)
        }
    }
    
    protected abstract fun getAllowedConnectionType(face: BlockFace): NetworkConnectionType
    
    protected abstract fun getConnectionType(face: BlockFace): NetworkConnectionType
    
    protected abstract fun setConnectionType(face: BlockFace, type: NetworkConnectionType)
    
    protected fun connectionConfigItem(side: BlockSide) = item {
        val (side, face) = getFaceFromSide(side)
        val connectionType = connectionTypes[face]!!
        
        val btnType = connectionType.map { type ->
            when (type) {
                NetworkConnectionType.NONE -> DefaultGuiItems.TP_GRAY_BTN
                NetworkConnectionType.EXTRACT -> DefaultGuiItems.TP_ORANGE_BTN
                NetworkConnectionType.INSERT -> DefaultGuiItems.TP_BLUE_BTN
                NetworkConnectionType.BUFFER -> DefaultGuiItems.TP_GREEN_BTN
            }
        }.flatten()
        itemProvider by itemProvider(btnType) {
            if (side != null) {
                name by Component.text()
                    .color(NamedTextColor.GRAY)
                    .append(Component.translatable("menu.nova.side_config.${side.name.lowercase()}"))
                    .append(Component.text(" ("))
                    .append(Component.translatable("menu.nova.side_config.${side.name.lowercase()}"))
                    .append(Component.text(")"))
                    .build()
            } else {
                name by Component.translatable("menu.nova.side_config.${face.name.lowercase()}", NamedTextColor.GRAY)
            }
            lore by connectionType.map { type ->
                listOf(when (type) {
                    NetworkConnectionType.NONE -> Component.translatable("menu.nova.side_config.none", NamedTextColor.GRAY)
                    NetworkConnectionType.EXTRACT -> Component.translatable("menu.nova.side_config.output", NamedTextColor.GOLD)
                    NetworkConnectionType.INSERT -> Component.translatable("menu.nova.side_config.input", NamedTextColor.AQUA)
                    NetworkConnectionType.BUFFER -> Component.translatable("menu.nova.side_config.input_output", NamedTextColor.GREEN)
                })
            }
        }
        
        onClick {
            player.playClickSound()
            queueCycleConnectionType(face, if (clickType.isLeftClick) 1 else -1)
        }
    }
    
    protected fun getFaceFromSide(blockSide: BlockSide): Pair<BlockSide?, BlockFace> {
        val facing = (endPoint as? TileEntity)?.blockState?.get(DefaultBlockStateProperties.FACING)
        return if (facing != null)
            blockSide to blockSide.getBlockFace(facing)
        else null to blockSide.getBlockFace(0f)
    }
    
}