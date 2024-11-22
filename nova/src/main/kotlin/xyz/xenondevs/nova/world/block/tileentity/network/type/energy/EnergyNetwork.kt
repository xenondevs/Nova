package xyz.xenondevs.nova.world.block.tileentity.network.type.energy

import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.collections.firstInstanceOfOrNull
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.map
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.config.node
import xyz.xenondevs.nova.util.sumOfNoOverflow
import xyz.xenondevs.nova.world.block.tileentity.network.Network
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkData
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.energy.holder.EnergyHolder
import java.util.BitSet
import kotlin.math.min
import kotlin.math.roundToLong

class EnergyNetwork internal constructor(
    networkData: NetworkData<EnergyNetwork>
) : Network<EnergyNetwork>, NetworkData<EnergyNetwork> by networkData {
    
    private val endPoints = ArrayList<NetworkEndPoint>()
    private val providers = ArrayList<EnergyHolder>()
    private val consumers = ArrayList<EnergyHolder>()
    private val buffers = ArrayList<EnergyHolder>()
    private val complexity: Int
    private val transferRate: Long
    
    init {
        var transferRate = DEFAULT_TRANSFER_RATE
        var complexity = 0
        
        for ((pos, con) in networkData.nodes) {
            val (node, faces) = con
            try {
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
                    
                    endPoints += node
                    complexity++
                } else if (node is EnergyBridge) {
                    transferRate = min(transferRate, node.energyTransferRate)
                }
            } catch (e: Exception) {
                throw Exception("Failed to add to energy network: $pos $con", e)
            }
        }
        
        this.transferRate = transferRate
        this.complexity = complexity
    }
    
    override fun isValid(): Boolean =
        endPoints.all { it.isValid }
    
    /**
     * Transfers energy between [providers], [consumers] and [buffers] in four steps:
     *
     * 1. Transfer energy from [providers] to [consumers], giving an equal amount of energy to every consumer
     * and taking an equal amount of energy from every provider, if possible.
     * 2. Transfer energy from [buffers] to [consumers], giving an equal amount of energy to every consumer
     * and taking an equal amount of energy from every buffer, if possible.
     * 3. Transfer energy from [providers] to [buffers], giving an equal amount of energy to every buffer
     * and taking an equal amount of energy from every provider, if possible.
     * 4. Balance the energy between [buffers] so that all buffers have the same percentage of energy filled.
     *
     * Depending on the [transferRate] and amount of energy transferred in each step, later steps might not be executed.
     */
    fun tick() {
        if (MAX_COMPLEXITY != -1 && complexity > MAX_COMPLEXITY)
            return
        
        var transferCapacity = transferRate
        val ignoredProviders = BitSet(providers.size)
        val ignoredConsumers = BitSet(consumers.size)
        
        // 1. transfer energy from providers to consumers
        if (providers.isNotEmpty() && consumers.isNotEmpty()) {
            val energy = min(transferCapacity, providers.sumOfNoOverflow { it.energy })
            val distributed = distributeEqually(energy, consumers, ignoredConsumers)
            takeEqually(distributed, providers, ignoredProviders)
            transferCapacity -= distributed
            if (transferCapacity <= 0)
                return
        }
        
        // 2. transfer energy from buffers to consumers
        if (buffers.isNotEmpty() && consumers.isNotEmpty()) {
            val energy = min(transferCapacity, buffers.sumOfNoOverflow { it.energy })
            val distributed = distributeEqually(energy, consumers, ignoredConsumers)
            takeEqually(distributed, buffers, BitSet(buffers.size))
            transferCapacity -= distributed
            if (transferCapacity <= 0)
                return
        }
        
        // 3. transfer energy from providers to buffers
        if (providers.isNotEmpty() && buffers.isNotEmpty()) {
            val energy = min(transferCapacity, providers.sumOfNoOverflow { it.energy })
            val distributed = distributeEqually(energy, buffers, BitSet(buffers.size))
            takeEqually(distributed, providers, ignoredProviders)
            transferCapacity -= distributed
            if (transferCapacity <= 0)
                return
        }
        
        // 4. balance buffers
        if (buffers.size >= 2) {
            balance(transferCapacity, buffers)
        }
    }
    
    private fun distributeEqually(energy: Long, consumers: List<EnergyHolder>, ignored: BitSet) =
        modifyEqually(energy, consumers, ignored, { it.maxEnergy - it.energy }, { holder, energy -> holder.energy += energy })
    
    private fun takeEqually(energy: Long, providers: List<EnergyHolder>, ignored: BitSet) =
        modifyEqually(energy, providers, ignored, EnergyHolder::energy) { holder, energy -> holder.energy -= energy }
    
    private inline fun modifyEqually(
        energy: Long,
        holders: List<EnergyHolder>,
        ignored: BitSet,
        value: (EnergyHolder) -> Long,
        apply: (EnergyHolder, Long) -> Unit
    ): Long {
        var remaining = energy
        
        while (remaining > 0) {
            val ignoredHolderCount = ignored.cardinality()
            if (ignoredHolderCount == holders.size)
                break
            
            val energyPerHolder = remaining / (holders.size - ignoredHolderCount)
            if (energyPerHolder <= 0)
                break
            
            for ((idx, holder) in holders.withIndex()) {
                if (ignored[idx])
                    continue
                
                val energyForHolder = min(energyPerHolder, value(holder))
                apply(holder, energyForHolder)
                remaining -= energyForHolder
                
                if (value(holder) <= 0)
                    ignored.set(idx)
            }
        }
        
        // The remaining energy is smaller than the non-ignored holder count and can thus not be distributed equally to all holders.
        // Instead, the remaining energy is just given to the first holders that can take it.
        if (remaining > 0) {
            for ((idx, holder) in holders.withIndex()) {
                if (ignored[idx])
                    continue
                
                val energyForHolder = min(remaining, value(holder))
                apply(holder, energyForHolder)
                remaining -= energyForHolder
                
                if (remaining <= 0)
                    break
            }
        }
        
        return energy - remaining
    }
    
    private fun balance(energy: Long, buffers: List<EnergyHolder>) {
        var transferCapacity = energy
        
        val totalEnergy = buffers.sumOfNoOverflow { it.energy }
        val totalCapacity = buffers.sumOfNoOverflow { it.maxEnergy }
        val targetFillFrac = totalEnergy.toDouble() / totalCapacity.toDouble()
        
        val sortedBuffers = buffers.toMutableList()
        while (transferCapacity > 0 && sortedBuffers.size >= 2) {
            sortedBuffers.sortBy { it.fillFrac }
            
            val lowestFillHolder = sortedBuffers.first()
            val highestFillHolder = sortedBuffers.last()
            val lowestFillFrac = lowestFillHolder.fillFrac
            val highestFillFrac = highestFillHolder.fillFrac
            
            // are buffers already balanced?
            if (highestFillFrac <= lowestFillFrac)
                break
            
            val available = ((highestFillHolder.fillFrac - targetFillFrac) * highestFillHolder.maxEnergy).toLong()
            val required = ((targetFillFrac - lowestFillHolder.fillFrac) * lowestFillHolder.maxEnergy).toLong()
            
            // the holders with highest/lowest fraction don't necessarily have the highest resolution
            // so we remove them from the list if they can't transfer any energy
            if (available <= 0 || required <= 0) {
                if (available <= 0)
                    sortedBuffers.removeLast()
                if (required <= 0)
                    sortedBuffers.removeFirst()
                
                continue
            }
            
            val transferAmount = minOf(transferCapacity, available, required)
            highestFillHolder.energy -= transferAmount
            lowestFillHolder.energy += transferAmount
            transferCapacity -= transferAmount
        }
    }
    
    private val EnergyHolder.fillFrac: Double
        get() = energy.toDouble() / maxEnergy.toDouble()
    
    override fun toString(): String {
        return "EnergyNetwork(nodes=$nodes)"
    }
    
    companion object {
        
        private val ENERGY_NETWORK = MAIN_CONFIG.node("network", "energy")
        val TICK_DELAY_PROVIDER: Provider<Int> = ENERGY_NETWORK.entry<Int>("tick_delay")
        val DEFAULT_TRANSFER_RATE: Long by combinedProvider(ENERGY_NETWORK.entry<Double>("default_transfer_rate"), TICK_DELAY_PROVIDER)
            .map { (defaultTransferRate, tickDelay) -> (defaultTransferRate * tickDelay).roundToLong() }
            .map { defaultTransferRate -> if (defaultTransferRate < 0) Long.MAX_VALUE else defaultTransferRate }
        val MAX_COMPLEXITY: Int by ENERGY_NETWORK.entry<Int>("max_complexity")
        
        internal fun validateLocal(from: NetworkEndPoint, to: NetworkEndPoint, face: BlockFace): Boolean {
            val itemHolderFrom = from.holders.firstInstanceOfOrNull<EnergyHolder>() ?: return false
            val itemHolderTo = to.holders.firstInstanceOfOrNull<EnergyHolder>() ?: return false
            val conFrom = itemHolderFrom.connectionConfig[face]
            val conTo = itemHolderTo.connectionConfig[face.oppositeFace]
            
            return conFrom != conTo || conFrom == NetworkConnectionType.BUFFER
        }
        
    }
    
}