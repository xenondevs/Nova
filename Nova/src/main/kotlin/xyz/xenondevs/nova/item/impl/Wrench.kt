package xyz.xenondevs.nova.item.impl

import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TranslatableComponent
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
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.network.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.network.energy.holder.EnergyHolder
import xyz.xenondevs.nova.tileentity.network.fluid.holder.FluidHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.util.concurrent.runIfTrue
import xyz.xenondevs.nova.util.isRightClick
import xyz.xenondevs.nova.util.swingHand
import xyz.xenondevs.nova.world.pos

internal object Wrench : ItemBehavior() {
    
    private val WRENCH_MODE_KEY = NamespacedKey(NOVA, "wrench_mode")
    
    private var ItemStack.wrenchMode: NetworkType
        get() = itemMeta?.persistentDataContainer
            ?.get(WRENCH_MODE_KEY, PersistentDataType.STRING)
            ?.let(NetworkType::valueOf)
            ?: NetworkType.ITEMS
        set(mode) {
            val itemMeta = itemMeta!!
            itemMeta.persistentDataContainer.set(WRENCH_MODE_KEY, PersistentDataType.STRING, mode.name)
            this.itemMeta = itemMeta
        }
    
    override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, event: PlayerInteractEvent) {
        if (action == Action.RIGHT_CLICK_BLOCK && !player.isSneaking) {
            val pos = event.clickedBlock!!.pos
            val endPoint = (TileEntityManager.getTileEntityAt(pos) ?: VanillaTileEntityManager.getTileEntityAt(pos)) as? NetworkEndPoint
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
                                NetworkType.ENERGY -> cycleEnergyConfig(holder as EnergyHolder, face)
                                NetworkType.ITEMS -> cycleItemConfig(holder as ItemHolder, face)
                                NetworkType.FLUID -> cycleFluidConfig(holder as FluidHolder, face)
                            }
                            
                            it.addEndPoint(endPoint, false).thenRun {
                                endPoint.updateNearbyBridges()
                            }
                        }
                    }
                }
                
            }
        } else if (action.isRightClick() && player.isSneaking) {
            val currentMode = itemStack.wrenchMode
            val newMode = NetworkType.values()[(currentMode.ordinal + 1) % NetworkType.values().size]
            itemStack.wrenchMode = newMode
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TranslatableComponent("item.nova.wrench.mode.${newMode.name.lowercase()}"))
        }
    }
    
    private fun cycleEnergyConfig(energyHolder: EnergyHolder, face: BlockFace) {
        val currentType = energyHolder.connectionConfig[face]!!
        val allowedTypes = energyHolder.allowedConnectionType.included
        
        val i = (allowedTypes.indexOf(currentType) + 1) % allowedTypes.size
        
        energyHolder.connectionConfig[face] = allowedTypes[i]
    }
    
    private fun cycleItemConfig(itemHolder: ItemHolder, face: BlockFace) {
        val currentType = itemHolder.connectionConfig[face]!!
        val inventory = itemHolder.inventories[face]!!
        val allowedTypes = itemHolder.allowedConnectionTypes[inventory]!!.included
        
        val i = (allowedTypes.indexOf(currentType) + 1) % allowedTypes.size
        
        itemHolder.connectionConfig[face] = allowedTypes[i]
    }
    
    private fun cycleFluidConfig(fluidHolder: FluidHolder, face: BlockFace) {
        val currentType = fluidHolder.connectionConfig[face]!!
        val container = fluidHolder.containerConfig[face]!!
        val allowedTypes = fluidHolder.allowedConnectionTypes[container]!!.included
        
        val i = (allowedTypes.indexOf(currentType) + 1) % allowedTypes.size
        
        fluidHolder.connectionConfig[face] = allowedTypes[i]
    }
    
}