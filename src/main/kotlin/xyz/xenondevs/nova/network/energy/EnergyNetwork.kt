package xyz.xenondevs.nova.network.energy

import com.google.common.base.Preconditions
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.network.*
import xyz.xenondevs.nova.network.energy.EnergyConnectionType.*
import xyz.xenondevs.particle.data.color.RegularColor
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/**
 * An EnergyNetwork consists of EnergyProviders, which provide
 * energy to the it and EnergyConsumers, which consume energy from it.
 *
 * Cables connect EnergyProviders to EnergyConsumers.
 */
class EnergyNetwork : Network {
    
    val color: RegularColor = RegularColor.random() // TODO: remove
    
    override val type = NetworkType.ENERGY
    
    override val nodes: Set<NetworkNode>
        get() = _nodes
    
    private val _nodes = HashSet<NetworkNode>()
    private val bridges = HashSet<EnergyBridge>()
    private val providers = HashSet<EnergyStorage>()
    private val consumers = HashSet<EnergyStorage>()
    private val buffers = HashSet<EnergyStorage>()
    
    private val availableProviderEnergy: Int
        get() = providers.sumOf { it.providedEnergy }
    private val availableBufferEnergy: Int
        get() = buffers.sumOf { it.providedEnergy }
    private val requestedConsumerEnergy: Int
        get() = consumers.sumOf { it.requestedEnergy }
    private val transferRate: Int
        get() = bridges.map { it.transferRate }.minOrNull() ?: 0
    
    override fun addAll(network: Network) {
        Preconditions.checkArgument(network is EnergyNetwork, "Illegal Network Type")
        network as EnergyNetwork
        
        _nodes += network._nodes
        providers += network.providers
        consumers += network.consumers
        bridges += network.bridges
        buffers += network.buffers
    }
    
    override fun addBridge(bridge: NetworkBridge) {
        Preconditions.checkArgument(bridge is EnergyBridge, "Illegal Bridge Type")
        _nodes += bridge
        bridges += bridge as EnergyBridge
    }
    
    override fun addEndPoint(endPoint: NetworkEndPoint, face: BlockFace) {
        Preconditions.checkArgument(endPoint is EnergyStorage, "Illegal EndPoint Type")
        
        when (val connectionType = (endPoint as EnergyStorage).energyConfig[face]!!) {
            
            PROVIDE -> if (!buffers.contains(endPoint)) providers += endPoint
            CONSUME -> if (!buffers.contains(endPoint)) consumers += endPoint
            BUFFER -> {
                removeNode(endPoint) // remove from provider / consumer set
                buffers += endPoint
            }
            else -> throw IllegalArgumentException("Illegal ConnectionType: $connectionType")
            
        }
        
        _nodes += endPoint
    }
    
    override fun removeNode(node: NetworkNode) {
        _nodes -= node
        if (node is EnergyStorage) {
            providers -= node
            consumers -= node
            buffers -= node
        } else if (node is EnergyBridge) {
            bridges -= node
        }
    }
    
    override fun isEmpty() = _nodes.isEmpty()
    
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
        if (energyDeficit != 0 && useBuffers) energyDeficit = takeEqually(energyDeficit, buffers)
        
        if (energyDeficit != 0) throw ArithmeticException("Not enough energy: $energyDeficit") // should never happen
    }
    
    private fun distributeEqually(energy: Int, consumers: Iterable<EnergyStorage>): Int {
        var availableEnergy = energy
        
        val consumerMap = ConcurrentHashMap<EnergyStorage, Int>()
        consumerMap += consumers
            .filterNot { it.requestedEnergy == 0 }
            .map { it to it.requestedEnergy }
        
        while (availableEnergy != 0 && consumerMap.isNotEmpty()) {
            val distribution = availableEnergy / consumerMap.size
            if (distribution == 0) break
            if (distribution != 0) {
                for ((consumer, requestedAmount) in consumerMap) {
                    val energyToGive = min(distribution, requestedAmount)
                    consumer.addEnergy(energyToGive)
                    if (energyToGive == requestedAmount) consumerMap -= consumer // consumer is satisfied
                    else consumerMap[consumer] = requestedAmount - energyToGive // consumer is not satisfied
                    availableEnergy -= energyToGive
                }
            } else {
                // can't split up equally
                return giveFirst(availableEnergy, consumers)
            }
        }
        
        return availableEnergy
    }
    
    private fun giveFirst(energy: Int, consumers: Iterable<EnergyStorage>): Int {
        var availableEnergy = energy
        for (consumer in consumers) {
            val energyToGive = min(availableEnergy, consumer.requestedEnergy)
            availableEnergy -= energyToGive
            consumer.addEnergy(energyToGive)
            
            if (availableEnergy == 0) break
        }
        
        return availableEnergy
    }
    
    private fun takeEqually(energy: Int, providers: Iterable<EnergyStorage>): Int {
        var energyDeficit = energy
        
        val providerMap = ConcurrentHashMap<EnergyStorage, Int>()
        providerMap += providers
            .filterNot { it.providedEnergy == 0 }
            .map { it to it.providedEnergy }
        
        while (energyDeficit != 0 && providerMap.isNotEmpty()) {
            val distribution = energyDeficit / providerMap.size
            if (distribution != 0) {
                for ((provider, providedAmount) in providerMap) {
                    val take = min(distribution, providedAmount)
                    energyDeficit -= take
                    provider.removeEnergy(take)
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
    
    private fun takeFirst(energy: Int, providers: Iterable<EnergyStorage>): Int {
        var energyDeficit = energy
        for (provider in providers) {
            val take = min(energyDeficit, provider.providedEnergy)
            energyDeficit -= take
            provider.removeEnergy(take)
            
            if (energyDeficit == 0) break
        }
        
        return energyDeficit
    }
    
}