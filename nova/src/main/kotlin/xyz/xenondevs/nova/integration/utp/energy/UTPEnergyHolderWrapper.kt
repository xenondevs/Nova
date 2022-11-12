package xyz.xenondevs.nova.integration.utp.energy

import io.github.unifiedtechpower.unified.energy.storage.EnergyConsumer
import io.github.unifiedtechpower.unified.energy.storage.EnergyProvider
import io.github.unifiedtechpower.unified.energy.storage.EnergyStorage
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.integration.utp.UTPNetworkEndPoint
import xyz.xenondevs.nova.integration.utp.energy.UTPEnergyNetworkManager.ENERGY_UNIT
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.EnergyHolder
import xyz.xenondevs.nova.util.enumMapOf

internal sealed class UTPEnergyHolderWrapper(
    override val endPoint: UTPNetworkEndPoint,
    private val energyStorage: EnergyStorage
) : EnergyHolder {
    
    override var energy: Long
        get() = energyStorage.getEnergy(ENERGY_UNIT)
        set(value) = energyStorage.setEnergy(ENERGY_UNIT, value)
    
    final override fun saveData() = Unit
    
    companion object {
        
        fun of(endPoint: UTPNetworkEndPoint, energyStorage: EnergyStorage): UTPEnergyHolderWrapper {
            return when {
                energyStorage is EnergyProvider && energyStorage is EnergyConsumer -> UTPEnergyBufferHolderWrapper(endPoint, energyStorage)
                energyStorage is EnergyProvider -> UTPEnergyProviderHolderWrapper(endPoint, energyStorage)
                energyStorage is EnergyConsumer -> UTPEnergyConsumerHolderWrapper(endPoint, energyStorage)
                else -> throw UnsupportedOperationException()
            }
        }
        
    }
    
}

internal class UTPEnergyProviderHolderWrapper(
    endPoint: UTPNetworkEndPoint,
    private val energyProvider: EnergyProvider
) : UTPEnergyHolderWrapper(endPoint, energyProvider) {
    
    override val allowedConnectionType = NetworkConnectionType.EXTRACT
    override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType>
        get() = energyProvider.providerBlockFaces.associateWithTo(enumMapOf()) { NetworkConnectionType.EXTRACT }
    
    override val requestedEnergy = 0L
    
}

internal class UTPEnergyConsumerHolderWrapper(
    endPoint: UTPNetworkEndPoint,
    private val energyConsumer: EnergyConsumer
) : UTPEnergyHolderWrapper(endPoint, energyConsumer) {
    
    override val allowedConnectionType = NetworkConnectionType.INSERT
    override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType>
        get() = energyConsumer.consumerBlockFaces.associateWithTo(enumMapOf()) { NetworkConnectionType.INSERT }
    
    override val requestedEnergy: Long
        get() = energyConsumer.getFreeSpace(ENERGY_UNIT).coerceAtMost(energyConsumer.maxEnergyInput)
    
}

internal class UTPEnergyBufferHolderWrapper<T>(
    endPoint: UTPNetworkEndPoint,
    private val energyBuffer: T
) : UTPEnergyHolderWrapper(endPoint, energyBuffer) where T : EnergyConsumer, T : EnergyProvider {
    
    override val allowedConnectionType = NetworkConnectionType.BUFFER
    override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType>
        get() {
            val map = energyBuffer.providerBlockFaces.associateWithTo(enumMapOf()) { NetworkConnectionType.EXTRACT }
            energyBuffer.consumerBlockFaces.forEach { face ->
                if (face in map)
                    map[face] = NetworkConnectionType.BUFFER
                else map[face] = NetworkConnectionType.EXTRACT
            }
            
            return map
        }
    
    override val requestedEnergy: Long
        get() = energyBuffer.getFreeSpace(ENERGY_UNIT).coerceAtMost(energyBuffer.maxEnergyInput)
    
}