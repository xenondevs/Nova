package xyz.xenondevs.nova.item.behavior.impl

import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.registry.NovaRegistries.NETWORK_TYPE
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.network.ContainerEndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.DefaultNetworkTypes
import xyz.xenondevs.nova.tileentity.network.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.network.energy.holder.EnergyHolder
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.util.concurrent.runIfTrue
import xyz.xenondevs.nova.util.get
import xyz.xenondevs.nova.util.toString
import xyz.xenondevs.nova.world.pos

internal object WrenchBehavior : ItemBehavior {
    
    private val WRENCH_MODE_KEY = NamespacedKey(NOVA, "wrench_mode")
    private val NETWORK_TYPES = arrayOf(DefaultNetworkTypes.ENERGY, DefaultNetworkTypes.ITEMS, DefaultNetworkTypes.FLUID)
    
    private var ItemStack.wrenchMode: NetworkType
        get() = itemMeta?.persistentDataContainer
            ?.get(WRENCH_MODE_KEY, PersistentDataType.STRING)
            ?.let(NETWORK_TYPE::get)
            ?: DefaultNetworkTypes.ITEMS
        set(mode) {
            val itemMeta = itemMeta!!
            itemMeta.persistentDataContainer.set(WRENCH_MODE_KEY, PersistentDataType.STRING, mode.id.toString())
            this.itemMeta = itemMeta
        }
    
    override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, event: PlayerInteractEvent) {
        if (action == Action.RIGHT_CLICK_BLOCK && !player.isSneaking) {
            val pos = event.clickedBlock!!.pos
            val endPoint = (TileEntityManager.getTileEntity(pos) ?: VanillaTileEntityManager.getTileEntityAt(pos)) as? NetworkEndPoint
            if (endPoint != null) {
                val mode = itemStack.wrenchMode
                val face = event.blockFace
                
                event.isCancelled = true
                player.swingHand(event.hand!!)
                
                ProtectionManager.canUseBlock(player, event.item, endPoint.location).runIfTrue {
                    NetworkManager.queueAsync {
                        val holder = endPoint.holders[mode]
                        if (holder != null) {
                            it.removeEndPoint(endPoint, false)
                            
                            when (mode) {
                                DefaultNetworkTypes.ENERGY -> cycleEnergyConfig(holder as EnergyHolder, face)
                                DefaultNetworkTypes.ITEMS, DefaultNetworkTypes.FLUID -> cycleContainerConfig(holder as ContainerEndPointDataHolder<*>, face)
                            }
                            
                            it.addEndPoint(endPoint, false).thenRun {
                                endPoint.updateNearbyBridges()
                            }
                        }
                    }
                }
                
            }
        } else if (action.isRightClick && player.isSneaking) {
            val currentMode = itemStack.wrenchMode
            val newMode = NETWORK_TYPES[(NETWORK_TYPES.indexOf(currentMode) + 1) % NETWORK_TYPES.size]
            itemStack.wrenchMode = newMode
            player.sendActionBar(Component.translatable("item.nova.wrench.mode.${newMode.id.toString(".")}"))
        }
    }
    
    private fun cycleEnergyConfig(energyHolder: EnergyHolder, face: BlockFace) {
        val currentType = energyHolder.connectionConfig[face]!!
        val allowedTypes = energyHolder.allowedConnectionType.included
        
        val i = (allowedTypes.indexOf(currentType) + 1) % allowedTypes.size
        
        energyHolder.connectionConfig[face] = allowedTypes[i]
    }
    
    private fun cycleContainerConfig(holder: ContainerEndPointDataHolder<*>, face: BlockFace) {
        val currentType = holder.connectionConfig[face]!!
        val inventory = holder.containerConfig[face]!!
        val allowedTypes = holder.allowedConnectionTypes[inventory]!!.included
        
        val i = (allowedTypes.indexOf(currentType) + 1) % allowedTypes.size
        
        holder.connectionConfig[face] = allowedTypes[i]
    }
    
}