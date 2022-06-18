package xyz.xenondevs.nova.tileentity.network

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.particleBuilder
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.particle.ParticleEffect
import java.awt.Color
import java.util.*

internal object NetworkDebugger {
    
    private val debuggers = HashMap<NetworkType, ArrayList<UUID>>()
    
    init {
        runTaskTimer(0, 1) { NetworkManager.tryExecute(::handleTick) }
    }
    
    fun toggleDebugger(type: NetworkType, player: Player) {
        val list = getViewerList(type)
        if (player.uniqueId in list) list -= player.uniqueId
        else list += player.uniqueId
    }
    
    private fun getViewerList(type: NetworkType): ArrayList<UUID> =
        debuggers.getOrPut(type, ::ArrayList)
    
    private fun handleTick(manager: NetworkManager) {
        if (debuggers.isEmpty() || debuggers.all { it.value.isEmpty() }) return
        
        manager.networks
            .forEach { network ->
                val players = getViewerList(network.type).mapNotNull(Bukkit::getPlayer)
                if (players.isEmpty()) return@forEach
                
                val color = Color(network.uuid.hashCode())
                
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