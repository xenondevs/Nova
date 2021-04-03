package xyz.xenondevs.nova.energy

import xyz.xenondevs.particle.data.color.RegularColor
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/**
 * An EnergyNetwork consists of EnergyProviders, which provide
 * energy to the it and EnergyConsumers, which consume energy from it.
 *
 * Cables connect EnergyProviders to EnergyConsumers.
 */
class EnergyNetwork() {
    
    val color: RegularColor = RegularColor.random() // TODO: remove
    
    private val nodes = HashSet<NetworkNode>()
    
    private val providers = HashSet<EnergyProvider>()
    private val consumers = HashSet<EnergyConsumer>()
    private val bridges = HashSet<EnergyBridge>()
    
    private val availableEnergy: Int
        get() = providers.map { it.providedEnergyAmount }.sum()
    private val transferRate: Int
        get() = bridges.map { it.transferRate }.minOrNull() ?: 0
    
    constructor(nodes: List<NetworkNode>) : this() {
        addNodes(nodes)
    }
    
    operator fun plusAssign(nodes: List<NetworkNode>) =
        addNodes(nodes)
    
    fun addNodes(nodes: List<NetworkNode>) =
        nodes.forEach(::addNode)
    
    operator fun plusAssign(node: NetworkNode) =
        addNode(node)
    
    fun addNode(node: NetworkNode) {
        nodes += node
        
        when (node) {
            is EnergyProvider -> providers += node
            is EnergyConsumer -> consumers += node
            is EnergyBridge -> bridges += node
        }
    }
    
    operator fun minusAssign(node: NetworkNode) =
        removeNode(node)
    
    fun removeNode(node: NetworkNode) {
        nodes -= node
        
        when (node) {
            is EnergyProvider -> providers -= node
            is EnergyConsumer -> consumers -= node
            is EnergyBridge -> bridges -= node
        }
    }
    
    fun getNodes() = ArrayList(nodes)
    
    fun isEmpty() = nodes.isEmpty()
    
    /**
     * Called every tick to transfer energy.
     */
    fun handleTick() {
        val providedEnergy = min(transferRate, availableEnergy)
        
        // equally distribute available energy
        var availableEnergy = providedEnergy
        
        val consumers = ConcurrentHashMap<EnergyConsumer, Int>()
        consumers += this.consumers
            .filterNot { it.requestedEnergyAmount == 0 }
            .map { it to it.requestedEnergyAmount }
            .toMap()
        
        while (availableEnergy != 0 && consumers.isNotEmpty()) {
            val distribution = availableEnergy / consumers.size
            if (distribution == 0) break
            for ((consumer, requestedAmount) in consumers) {
                val energyToGive = min(distribution, requestedAmount)
                consumer.consumeEnergy(energyToGive)
                if (energyToGive == requestedAmount) consumers -= consumer // consumer is satisfied
                else consumers[consumer] = requestedAmount - energyToGive // consumer is not satisfied
                availableEnergy -= energyToGive
            }
        }
        
        // take the energy that was actually consumed from the providers
        var energyTaken = providedEnergy - availableEnergy
        
        while (energyTaken != 0) {
            for (provider in providers) {
                val energyToTake = min(energyTaken, provider.providedEnergyAmount)
                provider.takeEnergy(energyToTake)
                energyTaken -= energyToTake
                
                if (energyTaken == 0) break
            }
        }
    }
    
}