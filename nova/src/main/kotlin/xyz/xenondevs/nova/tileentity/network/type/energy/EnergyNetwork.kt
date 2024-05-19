package xyz.xenondevs.nova.tileentity.network.type.energy

import xyz.xenondevs.commons.collections.firstInstanceOfOrNull
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.combinedProvider
import xyz.xenondevs.commons.provider.immutable.map
import xyz.xenondevs.nova.data.config.MAIN_CONFIG
import xyz.xenondevs.nova.tileentity.network.Network
import xyz.xenondevs.nova.tileentity.network.NetworkData
import xyz.xenondevs.nova.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.type.energy.holder.EnergyHolder
import xyz.xenondevs.nova.util.sumOfNoOverflow
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min
import kotlin.math.roundToLong

class EnergyNetwork(networkData: NetworkData<EnergyNetwork>) : Network<EnergyNetwork>, NetworkData<EnergyNetwork> by networkData {
    
    private val providers = HashSet<EnergyHolder>()
    private val consumers = HashSet<EnergyHolder>()
    private val buffers = HashSet<EnergyHolder>()
    
    private val transferRate: Long
    private val availableProviderEnergy: Long
        get() = providers.sumOfNoOverflow { it.energy }
    private val availableBufferEnergy: Long
        get() = buffers.sumOfNoOverflow { it.energy }
    private val requestedConsumerEnergy: Long
        get() = consumers.sumOfNoOverflow { it.requestedEnergy }
    
    init {
        var transferRate = DEFAULT_TRANSFER_RATE
        
        for ((node, faces) in networkData.nodes.values) {
            if (node is NetworkEndPoint) {
                val energyHolder = node.holders.firstInstanceOfOrNull<EnergyHolder>()
                    ?: continue
                
                var insert = false
                var extract = false
                for (face in faces) {
                    val connectionType = energyHolder.connectionConfig[face]
                        ?: throw IllegalArgumentException("Missing connection config for $face")
                    insert = insert || connectionType.insert
                    extract = extract || connectionType.extract
                }
                
                when (NetworkConnectionType.of(insert, extract)) {
                    NetworkConnectionType.BUFFER -> buffers += energyHolder
                    NetworkConnectionType.INSERT -> consumers += energyHolder
                    NetworkConnectionType.EXTRACT -> providers += energyHolder
                    else -> throw IllegalArgumentException("Invalid connection config for $energyHolder")
                }
            } else if (node is EnergyBridge) {
                transferRate = min(transferRate, node.energyTransferRate)
            }
        }
        
        this.transferRate = transferRate
    }
    
    /**
     * Called every tick to transfer energy.
     */
    fun tick() {
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
        
        if (energyDeficit != 0L) throw IllegalStateException("Not enough energy: $energyDeficit") // should never happen
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
    
    companion object {
        
        private val ENERGY_NETWORK = MAIN_CONFIG.node("network", "energy")
        val TICK_DELAY_PROVIDER: Provider<Int> = ENERGY_NETWORK.entry<Int>("tick_delay")
        val DEFAULT_TRANSFER_RATE: Long by combinedProvider(ENERGY_NETWORK.entry<Double>("default_transfer_rate"), TICK_DELAY_PROVIDER)
            .map { (defaultTransferRate, tickDelay) -> (defaultTransferRate * tickDelay).roundToLong() }
            .map { defaultTransferRate -> if (defaultTransferRate < 0) Long.MAX_VALUE else defaultTransferRate }
        
    }
    
}