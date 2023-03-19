package xyz.xenondevs.nova.ui.config.side

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.item.builder.addLoreLines
import xyz.xenondevs.invui.item.builder.setDisplayName
import xyz.xenondevs.invui.item.impl.AbstractItem
import xyz.xenondevs.nova.material.CoreGuiMaterial
import xyz.xenondevs.nova.tileentity.network.ContainerEndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.EndPointContainer
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.ui.item.BUTTON_COLORS
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.playClickSound

internal abstract class ContainerSideConfigGui<C : EndPointContainer, H : ContainerEndPointDataHolder<C>>(
    holder: H,
    containers: List<Pair<C, String>>
) : AbstractSideConfigGui<H>(holder) {
    
    protected abstract val hasSimpleVersion: Boolean
    protected abstract val hasAdvancedVersion: Boolean
    
    private val containers = containers.map { it.first }
    
    private val allowedConnectionTypes: Map<EndPointContainer, List<NetworkConnectionType>> =
        holder.allowedConnectionTypes.mapValues { (_, type) -> type.included }
    
    private val buttonBuilders: Map<EndPointContainer, ItemBuilder> =
        containers.withIndex().associate { (index, triple) ->
            triple.first to BUTTON_COLORS[index]
                .createClientsideItemBuilder()
                .addLoreLines(Component.translatable(triple.second, NamedTextColor.AQUA))
        }
    
    private var simpleModeBtn: SimplicityModeItem? = null
    
    private val simpleGui = Gui.normal()
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
    
    private val advancedGui = Gui.normal()
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
        .addIngredient('1', InventoryConfigItem(BlockSide.TOP))
        .addIngredient('2', InventoryConfigItem(BlockSide.LEFT))
        .addIngredient('3', InventoryConfigItem(BlockSide.FRONT))
        .addIngredient('4', InventoryConfigItem(BlockSide.RIGHT))
        .addIngredient('5', InventoryConfigItem(BlockSide.BOTTOM))
        .addIngredient('6', InventoryConfigItem(BlockSide.BACK))
        .build()
    
    fun initGui() {
        if (hasSimpleVersion && hasAdvancedVersion) {
            simpleModeBtn = SimplicityModeItem(true)
            advancedGui.setItem(8, 0, simpleModeBtn)
            
            val advancedModeBtn = SimplicityModeItem(false)
            simpleGui.setItem(8, 0, advancedModeBtn)
        }
        
        switchSimplicity(isSimpleConfiguration())
    }
    
    private fun switchSimplicity(simple: Boolean) {
        fillRectangle(0, 0, if (simple) simpleGui else advancedGui, true)
    }
    
    private fun changeInventory(blockFace: BlockFace, forward: Boolean): Boolean {
        if (containers.size <= 1) return false
        
        NetworkManager.execute { // TODO: runSync / runAsync ?
            it.removeEndPoint(holder.endPoint, false)
            
            val currentContainer = holder.containerConfig[blockFace]!!
            var index = containers.indexOf(currentContainer)
            index = (index + if (forward) 1 else -1).mod(containers.size)
            
            val newContainer = containers[index]
            holder.containerConfig[blockFace] = newContainer
            
            val allowedTypes = allowedConnectionTypes[newContainer]!!
            if (!allowedTypes.contains(holder.connectionConfig[blockFace]!!)) {
                holder.connectionConfig[blockFace] = allowedTypes[0]
            }
            
            it.addEndPoint(holder.endPoint)
        }
        
        simpleModeBtn?.notifyWindows()
        
        return true
    }
    
    override fun getAllowedConnectionTypes(blockFace: BlockFace): List<NetworkConnectionType> {
        return allowedConnectionTypes[holder.containerConfig[blockFace]!!]!!
    }
    
    abstract fun isSimpleConfiguration(): Boolean
    
    private inner class InventoryConfigItem(blockSide: BlockSide) : ConfigItem(blockSide) {
        
        override fun getItemProvider(): ItemProvider {
            val buttonBuilder = buttonBuilders[holder.containerConfig[blockFace]!!]!! // fixme: Unsafe network value access. Should only be accessed from NetworkManager thread.
            return buttonBuilder.setDisplayName(getSideName(blockSide, blockFace))
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            if (changeInventory(blockFace, clickType.isLeftClick)) {
                player.playClickSound()
                updateConfigItems(blockFace)
            }
        }
        
    }
    
    private inner class SimplicityModeItem(private val simple: Boolean) : AbstractItem() {
        
        override fun getItemProvider(): ItemProvider {
            return if (simple) {
                if (isSimpleConfiguration()) {
                    CoreGuiMaterial.SIMPLE_MODE_BTN_ON.clientsideProvider
                } else CoreGuiMaterial.SIMPLE_MODE_BTN_OFF.createClientsideItemBuilder()
                    .setDisplayName(Component.translatable("menu.nova.side_config.simple_mode"))
                    .addLoreLines(Component.translatable("menu.nova.side_config.simple_mode.unavailable", NamedTextColor.GRAY))
            } else CoreGuiMaterial.ADVANCED_MODE_BTN_ON.clientsideProvider
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            if (simple && !isSimpleConfiguration())
                return
            
            player.playClickSound()
            switchSimplicity(simple)
        }
        
    }
    
}