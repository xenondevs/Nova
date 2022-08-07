package xyz.xenondevs.nova.tileentity.network.energy

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.tileentity.network.Network
import xyz.xenondevs.nova.tileentity.network.NetworkBridge
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.NetworkException
import xyz.xenondevs.nova.tileentity.network.NetworkNode
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.network.energy.holder.EnergyHolder
import xyz.xenondevs.nova.util.sumOfNoOverflow
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

private val DEFAULT_TRANSFER_RATE by configReloadable { DEFAULT_CONFIG.getLong("network.energy.default_transfer_rate") }

/**
 * An EnergyNetwork consists of [NetworkBridge] that connect [NetworkEndPoint]
 * and their [EnergyHolder].<br>
 * [EnergyHolders][EnergyHolder] can provide, consume or buffer energy.
 */
class EnergyNetwork(override val uuid: UUID) : Network {
    
    override val type = NetworkType.ENERGY
    
    override val nodes: Set<NetworkNode>
        get() = _nodes
    
    private val _nodes = HashSet<NetworkNode>()
    private val bridges = HashSet<EnergyBridge>()
    private val providers = HashSet<EnergyHolder>()
    private val consumers = HashSet<EnergyHolder>()
    private val buffers = HashSet<EnergyHolder>()
    
    private val availableProviderEnergy: Long
        get() = providers.sumOfNoOverflow { it.energy }
    private val availableBufferEnergy: Long
        get() = buffers.sumOfNoOverflow { it.energy }
    private val requestedConsumerEnergy: Long
        get() = consumers.sumOfNoOverflow { it.requestedEnergy }
    private var transferRate = DEFAULT_TRANSFER_RATE
    
    override fun addAll(network: Network) {
        require(network !== this) { "Can't add to self" }
        require(network is EnergyNetwork) { "Illegal Network Type" }
        
        _nodes += network._nodes
        providers += network.providers
        consumers += network.consumers
        bridges += network.bridges
        buffers += network.buffers
    }
    
    override fun addAll(nodes: Iterable<Pair<BlockFace?, NetworkNode>>) {
        nodes.forEach { (face, node) ->
            if (node is NetworkBridge) addBridge(node)
            else if (node is NetworkEndPoint && face != null) addEndPoint(node, face)
        }
    }
    
    override fun addBridge(bridge: NetworkBridge) {
        require(bridge is EnergyBridge) { "Illegal Bridge Type" }
        _nodes += bridge
        bridges += bridge
        transferRate = bridge.energyTransferRate
    }
    
    override fun addEndPoint(endPoint: NetworkEndPoint, face: BlockFace) {
        val holder = endPoint.holders[NetworkType.ENERGY] as EnergyHolder
        
        when (val connectionType = holder.connectionConfig[face]!!) {
            
            NetworkConnectionType.EXTRACT -> {
                if (!buffers.contains(holder)) {
                    if (consumers.contains(holder)) {
                        consumers -= holder
                        buffers += holder
                    } else {
                        providers += holder
                    }
                }
            }
            
            NetworkConnectionType.INSERT -> {
                if (!buffers.contains(holder)) {
                    if (providers.contains(holder)) {
                        providers -= holder
                        buffers += holder
                    } else {
                        consumers += holder
                    }
                }
            }
            
            NetworkConnectionType.BUFFER -> {
                removeNode(endPoint) // remove from provider / consumer set
                buffers += holder
            }
            
            else -> throw IllegalArgumentException("Illegal ConnectionType: $connectionType")
            
        }
        
        _nodes += endPoint
    }
    
    override fun removeNode(node: NetworkNode) {
        _nodes -= node
        if (node is NetworkEndPoint) {
            val holder = node.holders[NetworkType.ENERGY] as EnergyHolder
            providers -= holder
            consumers -= holder
            buffers -= holder
        } else if (node is EnergyBridge) {
            bridges -= node
        }
    }
    
    override fun removeAll(nodes: List<NetworkNode>) {
        nodes.forEach { removeNode(it) }
    }
    
    override fun isEmpty() = _nodes.isEmpty()
    
    override fun isValid() = bridges.isNotEmpty() || ((providers.isNotEmpty() && consumers.isNotEmpty()) || (buffers.isNotEmpty() && (providers.isNotEmpty() || consumers.isNotEmpty())))
    
    override fun reload() {
        transferRate = bridges.firstOrNull()?.energyTransferRate ?: DEFAULT_TRANSFER_RATE
    }
    
    /**
     * Called every tick to transfer energy.
     */
    override fun handleTick() {
        val providerEnergy = min(transferRate, availableProviderEnergy)
        val bufferEnergy = min(transferRate - providerEnergy, availableBufferEnergy)
        val requestedEnergy = min(transferRate, requestedConsumerEnergy)
        
        val useBuffers = requestedEnergy > providerEnergy
        
        val availableEnergy = providerEnergy + if (useBuffers) bufferEnergy else 0
        
        var energy = availableEnergy
        energy = distributeEqually(energy, consumers)
        if (!useBuffers && energy > 0) energy = distributeEqually(energy, buffers) // didn't take energy from buffers, can fill them up
        
        var energyDeficit = availableEnergy - energy
        energyDeficit = takeEqually(energyDeficit, providers)
        if (energyDeficit != 0L && useBuffers) energyDeficit = takeEqually(energyDeficit, buffers)
        
        if (energyDeficit != 0L) throw NetworkException("Not enough energy: $energyDeficit") // should never happen
    }
    
    private fun distributeEqually(energy: Long, consumers: Iterable<EnergyHolder>): Long {
        var availableEnergy = energy
        
        val consumerMap = ConcurrentHashMap<EnergyHolder, Long>()
        consumerMap += consumers
            .filterNot { it.requestedEnergy == 0L }
            .map { it to it.requestedEnergy }
        
        while (availableEnergy != 0L && consumerMap.isNotEmpty()) {
            val distribution = availableEnergy / consumerMap.size
            if (distribution == 0L) break
            
            for ((consumer, requestedAmount) in consumerMap) {
                val energyToGive = min(distribution, requestedAmount)
                consumer.energy += energyToGive
                if (energyToGive == requestedAmount) consumerMap -= consumer // consumer is satisfied
                else consumerMap[consumer] = requestedAmount - energyToGive // consumer is not satisfied
                availableEnergy -= energyToGive
            }
        }
        
        return availableEnergy
    }
    
    private fun takeEqually(energy: Long, providers: Iterable<EnergyHolder>): Long {
        var energyDeficit = energy
        
        val providerMap = ConcurrentHashMap<EnergyHolder, Long>()
        providerMap += providers
            .filterNot { it.energy == 0L }
            .map { it to it.energy }
        
        while (energyDeficit != 0L && providerMap.isNotEmpty()) {
            val distribution = energyDeficit / providerMap.size
            if (distribution != 0L) {
                for ((provider, providedAmount) in providerMap) {
                    val take = min(distribution, providedAmount)
                    energyDeficit -= take
                    provider.energy -= take
                    if (take == providedAmount) providerMap -= provider // provider has no more energy
                    else providerMap[provider] = providedAmount - take // provider has less energy
                }
            } else {
                // can't split up equally
                return takeFirst(energyDeficit, providers)
            }
        }
        
        return energyDeficit
    }
    
    private fun takeFirst(energy: Long, providers: Iterable<EnergyHolder>): Long {
        var energyDeficit = energy
        for (provider in providers) {
            val take = min(energyDeficit, provider.energy)
            energyDeficit -= take
            provider.energy -= take
            
            if (energyDeficit == 0L) break
        }
        
        return energyDeficit
    }
    
}