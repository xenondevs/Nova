package xyz.xenondevs.nova.ui.menu.sideconfig

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.commons.collections.after
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.item.notifyWindows
import xyz.xenondevs.nova.ui.menu.item.AsyncItem
import xyz.xenondevs.nova.ui.menu.item.BUTTON_COLORS
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkManager
import xyz.xenondevs.nova.world.block.tileentity.network.node.ContainerEndPointDataHolder
import xyz.xenondevs.nova.world.block.tileentity.network.node.EndPointContainer
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.world.item.DefaultGuiItems

abstract class ContainerSideConfigMenu<C : EndPointContainer, H : ContainerEndPointDataHolder<C>> internal constructor(
    endPoint: NetworkEndPoint,
    networkType: NetworkType<*>,
    holder: H,
    private val namedContainers: Map<C, String>
) : AbstractSideConfigMenu<H>(endPoint, networkType, holder) {
    
    protected abstract val hasSimpleVersion: Boolean
    protected abstract val hasAdvancedVersion: Boolean
    
    private val containers: List<C> = namedContainers.keys.toList()
    private val containerConfigItems = enumMap<BlockFace, ContainerConfigItem>()
    private val simpleModeBtn = SimplicityModeItem(true)
    private val advancedModeBtn = SimplicityModeItem(false)
    
    private val simpleGui = Gui.builder()
        .setStructure(
            "# # # # u # # # #",
            "# # # l f r # # #",
            "# # # # d b # # #"
        )
        .addIngredient('u', ConnectionConfigItem(BlockSide.TOP))
        .addIngredient('l', ConnectionConfigItem(BlockSide.LEFT))
        .addIngredient('f', ConnectionConfigItem(BlockSide.FRONT))
        .addIngredient('r', ConnectionConfigItem(BlockSide.RIGHT))
        .addIngredient('d', ConnectionConfigItem(BlockSide.BOTTOM))
        .addIngredient('b', ConnectionConfigItem(BlockSide.BACK))
        .build()
    
    private val advancedGui = Gui.builder()
        .setStructure(
            "# # u # # # 1 # #",
            "# l f r # 2 3 4 #",
            "# # d b # # 5 6 #"
        )
        .addIngredient('u', ConnectionConfigItem(BlockSide.TOP))
        .addIngredient('l', ConnectionConfigItem(BlockSide.LEFT))
        .addIngredient('f', ConnectionConfigItem(BlockSide.FRONT))
        .addIngredient('r', ConnectionConfigItem(BlockSide.RIGHT))
        .addIngredient('d', ConnectionConfigItem(BlockSide.BOTTOM))
        .addIngredient('b', ConnectionConfigItem(BlockSide.BACK))
        .addIngredient('1', ContainerConfigItem(BlockSide.TOP))
        .addIngredient('2', ContainerConfigItem(BlockSide.LEFT))
        .addIngredient('3', ContainerConfigItem(BlockSide.FRONT))
        .addIngredient('4', ContainerConfigItem(BlockSide.RIGHT))
        .addIngredient('5', ContainerConfigItem(BlockSide.BOTTOM))
        .addIngredient('6', ContainerConfigItem(BlockSide.BACK))
        .build()
    
    override fun initAsync() {
        super.initAsync()
        val isSimple = isSimpleConfiguration()
        simpleModeBtn.updateAsync()
        advancedModeBtn.updateAsync()
        runTask {
            if (hasSimpleVersion && hasAdvancedVersion) {
                advancedGui.setItem(8, 0, simpleModeBtn)
                simpleGui.setItem(8, 0, advancedModeBtn)
            }
            switchSimplicity(isSimple)
        }
    }
    
    private fun switchSimplicity(simple: Boolean) {
        gui.fillRectangle(0, 0, if (simple) simpleGui else advancedGui, true)
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
            connectionConfigItems[face]?.forEach(AsyncItem::updateAsync)
            containerConfigItems[face]?.updateAsync()
            simpleModeBtn.updateAsync()
            runTask {
                connectionConfigItems[face]?.notifyWindows()
                containerConfigItems[face]?.notifyWindows()
                simpleModeBtn.notifyWindows()
            }
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
    
    private inner class ContainerConfigItem(side: BlockSide) : ConfigItem(side) {
        
        init {
            containerConfigItems[face] = this
        }
        
        override fun updateAsync() {
            val container = holder.containerConfig[face] ?: throw IllegalStateException("No container at $face")
            val color = BUTTON_COLORS[containers.indexOf(container)]
            
            val builder = color.createClientsideItemBuilder()
                .setName(getSideName(blockSide, face))
                .addLoreLines(Component.translatable(
                    namedContainers[container] ?: throw IllegalArgumentException("Missing name for $container"),
                    NamedTextColor.AQUA
                ))
            
            provider.set(builder)
        }
        
        override fun handleClick(clickType: ClickType, player: Player, click: Click) {
            player.playClickSound()
            queueCycleContainer(face, if (clickType.isLeftClick) 1 else -1)
        }
        
    }
    
    private inner class SimplicityModeItem(private val simple: Boolean) : AsyncItem() {
        
        override fun updateAsync() {
            val builder = if (simple) {
                if (isSimpleConfiguration()) {
                    DefaultGuiItems.SIMPLE_MODE_BTN_ON.clientsideProvider
                } else DefaultGuiItems.SIMPLE_MODE_BTN_OFF.createClientsideItemBuilder()
                    .setName(Component.translatable("menu.nova.side_config.simple_mode"))
                    .addLoreLines(Component.translatable("menu.nova.side_config.simple_mode.unavailable", NamedTextColor.GRAY))
            } else DefaultGuiItems.ADVANCED_MODE_BTN_ON.clientsideProvider
            
            provider.set(builder)
        }
        
        override fun handleClick(clickType: ClickType, player: Player, click: Click) {
            if (simple && !isSimpleConfiguration())
                return
            
            player.playClickSound()
            switchSimplicity(simple)
        }
        
    }
    
}