package xyz.xenondevs.nova.world.block.tileentity.network

import net.minecraft.core.particles.ParticleTypes
import org.bukkit.Bukkit
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import xyz.xenondevs.commons.collections.takeUnlessEmpty
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.filterInRange
import xyz.xenondevs.nova.util.particle.color
import xyz.xenondevs.nova.util.particle.particle
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.util.sendTo
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkBridge
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkType
import java.awt.Color
import java.util.*

internal object NetworkDebugger {
    
    private val networkDebuggers = HashMap<NetworkType<*>, HashSet<UUID>>()
    private val clusterDebuggers = HashSet<UUID>()
    
    init {
        runTaskTimer(0, 1, ::handleTick)
    }
    
    fun toggleDebugger(type: NetworkType<*>, player: Player): Boolean {
        val typeDebuggers = networkDebuggers.getOrPut(type, ::HashSet)
        if (player.uniqueId in typeDebuggers) {
            typeDebuggers -= player.uniqueId
            if (typeDebuggers.isEmpty())
                networkDebuggers -= type
            
            return false
        } else {
            typeDebuggers += player.uniqueId
            return true
        }
    }
    
    fun toggleClusterDebugger(player: Player): Boolean {
        if (player.uniqueId in clusterDebuggers) {
            clusterDebuggers -= player.uniqueId
            return false
        } else {
            clusterDebuggers += player.uniqueId
            return true
        }
    }
    
    private fun handleTick() {
        tickNetworkDebuggers()
        tickClusterDebuggers()
    }
    
    private fun tickNetworkDebuggers() {
        if (networkDebuggers.isEmpty())
            return
        
        for (network in NetworkManager.networks) {
            val players = networkDebuggers[network.type]
                ?.mapNotNull(Bukkit::getPlayer)
                ?.takeUnlessEmpty()
                ?: continue
            
            showNetwork(Color(network.uuid.hashCode()), network, players)
        }
    }
    
    private fun tickClusterDebuggers() {
        if (clusterDebuggers.isEmpty())
            return
        
        val players = clusterDebuggers.mapNotNull(Bukkit::getPlayer)
        if (players.isEmpty())
            return
        
        for (cluster in NetworkManager.clusters) {
            val color = Color(cluster.uuid.hashCode())
            for (network in cluster.networks) {
                showNetwork(color, network, players)
            }
        }
    }
    
    private fun showNetwork(color: Color, network: Network<*>, players: List<Player>) {
        for ((node, faces) in network.nodes.values) {
            val receivers = players.filterInRange(node.pos.location, 64.0)
            if (receivers.isEmpty())
                continue
            
            when (node) {
                is NetworkBridge -> showNetworkBridge(node, color, receivers)
                is NetworkEndPoint -> faces.forEach { showNetworkEndPoint(node, it, color, receivers) }
            }
        }
    }
    
    private fun showNetworkBridge(bridge: NetworkBridge, color: Color, players: List<Player>) {
        val particleLocation = bridge.pos.location.add(0.5, 0.5, 0.5)
        particle(ParticleTypes.DUST, particleLocation) { color(color) }.sendTo(players)
    }
    
    private fun showNetworkEndPoint(endPoint: NetworkEndPoint, face: BlockFace, color: Color, players: List<Player>) {
        val particleLocation = endPoint.pos.location
            .add(0.5, 0.5, 0.5)
            .advance(face, 0.5)
        particle(ParticleTypes.DUST, particleLocation) { color(color) }.sendTo(players)
    }
    
}