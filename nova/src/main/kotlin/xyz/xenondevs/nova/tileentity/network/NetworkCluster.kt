package xyz.xenondevs.nova.tileentity.network

import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.tileentity.network.node.NetworkNode
import java.util.*
import java.util.logging.Level

class NetworkCluster(val uuid: UUID, val networks: Collection<Network>) {
    
    // It might make sense to add a random offset to the current tick count in order to spread out the network ticks
    // of networks with tick delay > 1, but this would create inconsistent transfer rates in cases where clusters
    // are rebuilt (thus regenerating the offset) very often.
    
    fun tickNetworks(tick: Int) {
        for (network in networks) {
            val tickDelay = network.type.tickDelay
            if (tick % tickDelay == 0) {
                try {
                    network.handleTick()
                } catch (e: Exception) {
                    LOGGER.log(Level.SEVERE, "An exception occurred trying to tick $network in group $this", e)
                }
            }
        }
    }
    
}

/**
 * A group of [ProtoNetworks][ProtoNetwork] that share at least one [NetworkNode].
 *
 * [ProtoNetworkClusters][ProtoNetworkCluster] are used to build [NetworkClusters][NetworkCluster],
 * which are groups of [Networks][Network] that are ticked together.
 */
class ProtoNetworkCluster : Iterable<ProtoNetwork> {
    
    val uuid: UUID = UUID.randomUUID()
    private val networks = HashSet<ProtoNetwork>()
    
    operator fun plusAssign(network: ProtoNetwork) {
        networks += network
    }
    
    operator fun plusAssign(networks: Iterable<ProtoNetwork>) {
        this.networks += networks
    }
    
    operator fun plusAssign(other: ProtoNetworkCluster?) {
        if (other == null)
            return
        
        networks += other.networks
    }
    
    operator fun minusAssign(network: ProtoNetwork) {
        networks -= network
    }
    
    operator fun contains(network: ProtoNetwork) =
        network in networks
    
    override operator fun iterator() =
        networks.iterator()
    
}