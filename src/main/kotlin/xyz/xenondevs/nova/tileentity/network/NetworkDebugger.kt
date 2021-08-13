package xyz.xenondevs.nova.tileentity.network

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import xyz.xenondevs.nova.tileentity.network.energy.EnergyNetwork
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.particleBuilder
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.particle.ParticleEffect
import java.awt.Color
import java.util.*

object NetworkDebugger {
    
    private val energyDebuggers = ArrayList<UUID>()
    private val itemDebuggers = ArrayList<UUID>()
    
    init {
        runTaskTimer(0, 1, NetworkDebugger::handleTick)
    }
    
    fun toggleDebugger(type: NetworkType, player: Player) {
        when (type) {
            NetworkType.ENERGY -> {
                if (energyDebuggers.contains(player.uniqueId)) energyDebuggers -= player.uniqueId
                else energyDebuggers += player.uniqueId
            }
            
            NetworkType.ITEMS -> {
                if (itemDebuggers.contains(player.uniqueId)) itemDebuggers -= player.uniqueId
                else itemDebuggers += player.uniqueId
            }
        }
    }
    
    private fun handleTick() {
        if (energyDebuggers.isEmpty() && itemDebuggers.isEmpty()) return
        
        NetworkManager.networks
            .forEach { network ->
                val players = if (network is EnergyNetwork) energyDebuggers.mapNotNull(Bukkit::getPlayer) else itemDebuggers.mapNotNull(Bukkit::getPlayer)
                if (players.isEmpty()) return@forEach
                
                val color = Color(network.hashCode())
                
                network.nodes.forEach { node ->
                    if (node is NetworkBridge) {
                        showNetworkPoint(node.location, null, color, players)
                    } else if (node is NetworkEndPoint) {
                        node.getNetworks()
                            .filter { it.second == network }
                            .forEach { showNetworkPoint(node.location, it.first, color, players) }
                    }
                }
            }
    }
    
    private fun showNetworkPoint(location: Location, blockFace: BlockFace?, color: Color, players: List<Player>) {
        val particleLocation = location.clone().apply { add(0.5, 0.0, 0.5) }
        if (blockFace != null)
            particleLocation.add(0.0, 0.5, 0.0).advance(blockFace, 0.5)
        
        particleBuilder(ParticleEffect.REDSTONE, particleLocation) {
            color(color)
        }.display(players)
    }
    
}