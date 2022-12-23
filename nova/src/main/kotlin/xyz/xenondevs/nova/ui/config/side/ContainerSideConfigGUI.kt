package xyz.xenondevs.nova.ui.config.side

import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.builder.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.CoreGUIMaterial
import xyz.xenondevs.nova.tileentity.network.ContainerEndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.EndPointContainer
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.ui.item.BUTTON_COLORS
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.data.addLocalizedLoreLines
import xyz.xenondevs.nova.util.data.addLoreLines
import xyz.xenondevs.nova.util.data.setLocalizedName
import xyz.xenondevs.nova.util.playClickSound

internal abstract class ContainerSideConfigGUI<C : EndPointContainer, H : ContainerEndPointDataHolder<C>>(
    holder: H,
    containers: List<Pair<C, String>>
) : BaseSideConfigGUI<H>(holder) {
    
    protected abstract val hasSimpleVersion: Boolean
    
    private val containers = containers.map { it.first }
    
    private val allowedConnectionTypes: Map<EndPointContainer, List<NetworkConnectionType>> =
        holder.allowedConnectionTypes.mapValues { (_, type) -> type.included }
    
    private val buttonBuilders: Map<EndPointContainer, ItemBuilder> =
        containers.withIndex().associate { (index, triple) ->
            triple.first to BUTTON_COLORS[index]
                .createClientsideItemBuilder()
                .addLoreLines(TranslatableComponent(triple.second).apply { color = ChatColor.AQUA })
        }
    
    private var simpleModeBtn: SimplicityModeItem? = null
    
    private val simpleGUI = GUIBuilder(GUIType.NORMAL)
        .setStructure(
            "# # # # u # # # a",
            "# # # l f r # # #",
            "# # # # d b # # #"
        )
        .addIngredient('a', SimplicityModeItem(false))
        .addIngredient('u', ConnectionConfigItem(BlockSide.TOP))
        .addIngredient('l', ConnectionConfigItem(BlockSide.LEFT))
        .addIngredient('f', ConnectionConfigItem(BlockSide.FRONT))
        .addIngredient('r', ConnectionConfigItem(BlockSide.RIGHT))
        .addIngredient('d', ConnectionConfigItem(BlockSide.BOTTOM))
        .addIngredient('b', ConnectionConfigItem(BlockSide.BACK))
        .build()
    
    private val advancedGUI = GUIBuilder(GUIType.NORMAL)
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
    
    fun initGUI() {
        if (hasSimpleVersion) {
            val btn = SimplicityModeItem(true)
            advancedGUI.setItem(8, 0, btn)
            simpleModeBtn = btn
        }
        
        switchSimplicity(isSimpleConfiguration())
    }
    
    private fun switchSimplicity(simple: Boolean) {
        fillRectangle(0, 0, if (simple) simpleGUI else advancedGUI, true)
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
            return buttonBuilder.setDisplayName(*getSideName(blockSide, blockFace))
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            if (changeInventory(blockFace, clickType.isLeftClick)) {
                player.playClickSound()
                updateConfigItems(blockFace)
            }
        }
        
    }
    
    private inner class SimplicityModeItem(private val simple: Boolean) : BaseItem() {
        
        override fun getItemProvider(): ItemProvider {
            return if (simple) {
                if (isSimpleConfiguration()) {
                    CoreGUIMaterial.SIMPLE_MODE_BTN_ON.clientsideProvider
                } else CoreGUIMaterial.SIMPLE_MODE_BTN_OFF.createClientsideItemBuilder()
                    .setLocalizedName("menu.nova.side_config.simple_mode")
                    .addLocalizedLoreLines(ChatColor.GRAY, "menu.nova.side_config.simple_mode.unavailable")
            } else CoreGUIMaterial.ADVANCED_MODE_BTN_ON.clientsideProvider
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            if (simple && !isSimpleConfiguration())
                return
            
            player.playClickSound()
            switchSimplicity(simple)
        }
        
    }
    
}