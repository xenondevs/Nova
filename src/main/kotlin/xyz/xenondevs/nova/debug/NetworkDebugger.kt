package xyz.xenondevs.nova.debug

import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.NetworkBridge
import xyz.xenondevs.nova.network.NetworkEndPoint
import xyz.xenondevs.nova.network.NetworkManager
import xyz.xenondevs.nova.network.NetworkType
import xyz.xenondevs.nova.network.energy.EnergyNetwork
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.particle.ParticleBuilder
import xyz.xenondevs.particle.ParticleEffect
import xyz.xenondevs.particle.data.color.RegularColor
import java.awt.Color

object NetworkDebugger {
    
    private val energyDebuggers = ArrayList<Player>()
    private val itemDebuggers = ArrayList<Player>()
    
    init {
        runTaskTimer(0, 1, ::handleTick)
    }
    
    fun toggleDebugger(type: NetworkType, player: Player) {
        when (type) {
            NetworkType.ENERGY -> {
                if (energyDebuggers.contains(player)) energyDebuggers -= player
                else energyDebuggers += player
            }
            
            NetworkType.ITEMS -> {
                if (itemDebuggers.contains(player)) itemDebuggers -= player
                else itemDebuggers += player
            }
        }
    }
    
    private fun handleTick() {
        if (energyDebuggers.isEmpty() && itemDebuggers.isEmpty()) return
        
        NetworkManager.networks
            .forEach { network ->
                val players = if (network is EnergyNetwork) energyDebuggers else itemDebuggers
                val colorHex = Integer.toHexString(network.hashCode() % Integer.parseInt("ffffff", 16))
                val color = Color.decode("#$colorHex")
                
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
        
        ParticleBuilder(ParticleEffect.REDSTONE, particleLocation)
            .setParticleData(RegularColor(color))
            .display(players)
    }
    
}