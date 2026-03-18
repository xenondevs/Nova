package xyz.xenondevs.nova.ui.menu.sideconfig

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.collections.after
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.flatten
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.invui.dsl.gui
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.dsl.itemProvider
import xyz.xenondevs.invui.dsl.tabGui
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.ui.menu.by
import xyz.xenondevs.nova.ui.menu.item.TP_BUTTON_COLORS
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkManager
import xyz.xenondevs.nova.world.block.tileentity.network.node.ContainerEndPointDataHolder
import xyz.xenondevs.nova.world.block.tileentity.network.node.EndPointContainer
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.world.format.NetworkState
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.clientsideProvider

abstract class ContainerSideConfigMenu<C : EndPointContainer, H : ContainerEndPointDataHolder<C>> internal constructor(
    endPoint: NetworkEndPoint,
    networkType: RegistryEntry.Nova<NetworkType<*>>,
    holder: H,
    protected val namedContainers: Map<C, String>
) : AbstractSideConfigMenu<H>(endPoint, networkType, holder) {
    
    private val containers: List<C> = namedContainers.keys.toList()
    
    protected val simpleMode = mutableProvider(SimplicityMode.ADVANCED)
    private val isSimpleConfiguration = mutableProvider(false)
    private val containersAtFace: Map<BlockFace, MutableProvider<C?>> =
        CUBE_FACES.associateWith { mutableProvider(null) }
    
    override val gui = tabGui(
        "x x x x x x x x",
        "x x x x x x x x",
        "x x x x x x x x",
        "x x x x x x x x",
    ) {
        'u' by connectionConfigItem(BlockSide.TOP)
        'l' by connectionConfigItem(BlockSide.LEFT)
        'f' by connectionConfigItem(BlockSide.FRONT)
        'r' by connectionConfigItem(BlockSide.RIGHT)
        'd' by connectionConfigItem(BlockSide.BOTTOM)
        'b' by connectionConfigItem(BlockSide.BACK)
        '1' by containerConfigItem(BlockSide.TOP)
        '2' by containerConfigItem(BlockSide.LEFT)
        '3' by containerConfigItem(BlockSide.FRONT)
        '4' by containerConfigItem(BlockSide.RIGHT)
        '5' by containerConfigItem(BlockSide.BOTTOM)
        '6' by containerConfigItem(BlockSide.BACK)
        's' by simplicityModeItem(simpleMode, isSimpleConfiguration)
        
        val simpleGui = gui(
            ". . . . . . . .",
            ". . . . u . . s",
            ". . . l f r . .",
            ". . . . d b . ."
        ) {}
        
        val advancedGui = gui(
            ". . . . . . . .",
            ". . u . . . 1 s",
            ". l f r . 2 3 4",
            ". . d b . . 5 6"
        ) {}
        
        tabs by listOf(simpleGui, advancedGui)
        tab by simpleMode.map(SimplicityMode::tab, SimplicityMode.entries::get)
    }
    
    override fun refresh(state: NetworkState) {
        super.refresh(state)
        isSimpleConfiguration.set(isSimpleConfiguration())
        containersAtFace.forEach { (face, container) -> container.set(holder.containerConfig[face]) }
    }
    
    private fun queueCycleContainer(face: BlockFace, move: Int) {
        if (containers.size <= 1)
            return
        
        NetworkManager.queueWrite(endPoint.pos.chunkPos) { state ->
            // cycle container
            val currentContainer = holder.containerConfig[face]!!
            val newContainer = containers.after(currentContainer, move)
            holder.containerConfig[face] = newContainer
            // adjust connection type
            val allowedTypes = holder.containers[newContainer]!!.supertypes
            if (holder.connectionConfig[face] !in allowedTypes)
                holder.connectionConfig[face] = allowedTypes[0]
            
            state.getNetwork(endPoint, networkType, face)?.markDirty()
            state.handleEndPointAllowedFacesChange(endPoint, networkType, face)
            
            // update ui
            refresh(state)
        }
    }
    
    override fun getAllowedConnectionType(face: BlockFace): NetworkConnectionType =
        holder.containerConfig[face]
            ?.let { holder.containers[it] }
            ?.takeUnless { face in holder.blockedFaces }
            ?: NetworkConnectionType.NONE
    
    override fun getConnectionType(face: BlockFace): NetworkConnectionType {
        return holder.connectionConfig[face]!!
    }
    
    override fun setConnectionType(face: BlockFace, type: NetworkConnectionType) {
        holder.connectionConfig[face] = type
    }
    
    protected abstract fun isSimpleConfiguration(): Boolean
    
    private fun containerConfigItem(side: BlockSide) = item {
        val (_, face) = getFaceFromSide(side)
        
        itemProvider by itemProvider {
            type by containersAtFace[face]!!.map { container ->
                if (container != null)
                    TP_BUTTON_COLORS[containers.indexOf(container)]
                else DefaultGuiItems.GRAY_BTN
            }
            name by containersAtFace[face]!!.map { container ->
                Component.translatable(
                    namedContainers[container] ?: "",
                    NamedTextColor.AQUA
                )
            }
        }
        
        onClick {
            player.playClickSound()
            queueCycleContainer(face, if (clickType.isLeftClick) 1 else -1)
        }
    }
    
    private fun simplicityModeItem(
        currentMode: MutableProvider<SimplicityMode>,
        isSimpleConfiguration: Provider<Boolean>
    ) = item {
        itemProvider by combinedProvider(
            currentMode, isSimpleConfiguration
        ) { currentMode, isSimpleConfiguration ->
            when (currentMode) {
                SimplicityMode.SIMPLE_ONLY, SimplicityMode.ADVANCED_ONLY -> DefaultGuiItems.INVISIBLE_ITEM.clientsideProvider
                SimplicityMode.ADVANCED if isSimpleConfiguration -> DefaultGuiItems.TP_SMALL_SIMPLE_MODE_BTN_ON.clientsideProvider
                SimplicityMode.ADVANCED -> DefaultGuiItems.TP_SMALL_SIMPLE_MODE_BTN_OFF.clientsideProvider
                else -> DefaultGuiItems.TP_SMALL_ADVANCED_MODE_BTN_ON.clientsideProvider
            }
        }.flatten()
        
        onClick {
            if (currentMode.get().canSwitch(isSimpleConfiguration.get())) {
                player.playClickSound()
                currentMode.set(!currentMode.get())
            }
        }
    }
    
    protected enum class SimplicityMode(val tab: Int) {
        
        SIMPLE(0),
        SIMPLE_ONLY(0),
        ADVANCED(1),
        ADVANCED_ONLY(1);
        
        fun canSwitch(isSimpleConfiguration: Boolean): Boolean = when (this) {
            SIMPLE -> true
            SIMPLE_ONLY -> false
            ADVANCED -> isSimpleConfiguration
            ADVANCED_ONLY -> false
        }
        
        operator fun not(): SimplicityMode = when (this) {
            SIMPLE -> ADVANCED
            SIMPLE_ONLY -> SIMPLE_ONLY
            ADVANCED -> SIMPLE
            ADVANCED_ONLY -> ADVANCED_ONLY
        }
        
    }
}
