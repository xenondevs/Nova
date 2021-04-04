package xyz.xenondevs.nova.energy

import xyz.xenondevs.nova.energy.EnergyConnectionType.*
import xyz.xenondevs.particle.data.color.RegularColor
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/**
 * An EnergyNetwork consists of EnergyProviders, which provide
 * energy to the it and EnergyConsumers, which consume energy from it.
 *
 * Cables connect EnergyProviders to EnergyConsumers.
 */
class EnergyNetwork {
    
    val color: RegularColor = RegularColor.random() // TODO: remove
    
    private val _nodes = HashSet<EnergyNode>()
    val nodes: Set<EnergyNode>
        get() = _nodes
    
    private val providers = HashSet<EnergyStorage>()
    private val consumers = HashSet<EnergyStorage>()
    private val bridges = HashSet<EnergyBridge>()
    
    private val availableEnergy: Int
        get() = providers.map { it.providedEnergy }.sum()
    private val transferRate: Int
        get() = bridges.map { it.transferRate }.minOrNull() ?: 0
    
    fun addAll(network: EnergyNetwork) {
        _nodes += network._nodes
        providers += network.providers
        consumers += network.consumers
        bridges += network.bridges
    }
    
    fun addBridge(bridge: EnergyBridge) {
        _nodes += bridge
        bridges += bridge
    }
    
    fun addStorage(storage: EnergyStorage, connectionType: EnergyConnectionType) {
        when (connectionType) {
            PROVIDE -> providers += storage
            CONSUME -> consumers += storage
            BUFFER -> {
                providers += storage
                consumers += storage
            }
            else -> throw IllegalArgumentException("Illegal ConnectionType: $connectionType")
        }
        _nodes += storage
    }
    
    operator fun minusAssign(node: EnergyNode) =
        removeNode(node)
    
    fun removeNode(node: EnergyNode) {
        _nodes -= node
        if (node is EnergyStorage) {
            providers -= node
            consumers -= node
        }
    }
    
    fun isEmpty() = _nodes.isEmpty()
    
    /**
     * Called every tick to transfer energy.
     */
    fun handleTick() {
        val providedEnergy = min(transferRate, availableEnergy)
        
        // equally distribute available energy
        var availableEnergy = providedEnergy
        
        val consumers = ConcurrentHashMap<EnergyStorage, Int>()
        consumers += this.consumers
            .filterNot { it.requestedEnergy == 0 }
            .map { it to it.requestedEnergy }
            .toMap()
        
        while (availableEnergy != 0 && consumers.isNotEmpty()) {
            val distribution = availableEnergy / consumers.size
            if (distribution == 0) break
            for ((consumer, requestedAmount) in consumers) {
                val energyToGive = min(distribution, requestedAmount)
                consumer.addEnergy(energyToGive)
                if (energyToGive == requestedAmount) consumers -= consumer // consumer is satisfied
                else consumers[consumer] = requestedAmount - energyToGive // consumer is not satisfied
                availableEnergy -= energyToGive
            }
        }
        
        // take the energy that was actually consumed from the providers
        var energyTaken = providedEnergy - availableEnergy
        
        while (energyTaken != 0) {
            for (provider in providers) {
                val energyToTake = min(energyTaken, provider.providedEnergy)
                provider.removeEnergy(energyToTake)
                energyTaken -= energyToTake
                
                if (energyTaken == 0) break
            }
        }
    }
    
}