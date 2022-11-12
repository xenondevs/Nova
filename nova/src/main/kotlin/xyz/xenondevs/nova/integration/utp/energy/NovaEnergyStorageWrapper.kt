package xyz.xenondevs.nova.integration.utp.energy

import io.github.unifiedtechpower.unified.energy.storage.EnergyConsumer
import io.github.unifiedtechpower.unified.energy.storage.EnergyProvider
import io.github.unifiedtechpower.unified.energy.storage.EnergyStorage
import io.github.unifiedtechpower.unified.energy.unit.EnergyUnit
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.integration.utp.energy.UTPEnergyNetworkManager.ENERGY_UNIT
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.BufferEnergyHolder
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.network.energy.holder.NovaEnergyHolder
import xyz.xenondevs.nova.tileentity.network.energy.holder.ProviderEnergyHolder
import kotlin.math.min

internal sealed class NovaEnergyStorageWrapper(val holder: NovaEnergyHolder) : EnergyStorage {
    
    override fun getMaxEnergy(): Long {
        return holder.maxEnergy
    }
    
    override fun getEnergy(): Long {
        return holder.energy
    }
    
    override fun setEnergy(energy: Long) {
        holder.energy = energy
    }
    
    override fun getEnergyUnit(): EnergyUnit = ENERGY_UNIT
    
}

internal class NovaEnergyProviderWrapper(holder: ProviderEnergyHolder) : NovaEnergyStorageWrapper(holder), EnergyProvider {
    
    override fun extractEnergy(energy: Long): Long {
        val minus = min(holder.energy, energy)
        holder.energy -= minus
        return minus
    }
    
    override fun getMaxEnergyOutput(): Long {
        return Long.MAX_VALUE
    }
    
    override fun getProviderBlockFaces(): MutableList<BlockFace> {
        return holder.connectionConfig.mapNotNullTo(ArrayList()) { if (it.value == NetworkConnectionType.EXTRACT) it.key else null }
    }
    
}

internal class NovaEnergyConsumerWrapper(holder: ConsumerEnergyHolder) : NovaEnergyStorageWrapper(holder), EnergyConsumer {
    
    override fun insertEnergy(energy: Long): Long {
        val plus = min(energy, holder.maxEnergy - holder.energy)
        holder.energy += plus
        return plus
    }
    
    override fun getMaxEnergyInput(): Long {
        return Long.MAX_VALUE
    }
    
    override fun getConsumerBlockFaces(): MutableList<BlockFace> {
        return holder.connectionConfig.mapNotNullTo(ArrayList()) { if (it.value == NetworkConnectionType.INSERT) it.key else null }
    }
    
}

internal class NovaEnergyBufferWrapper(holder: BufferEnergyHolder) : NovaEnergyStorageWrapper(holder), EnergyProvider, EnergyConsumer {
   
    override fun insertEnergy(energy: Long): Long {
        val plus = min(energy, holder.maxEnergy - holder.energy)
        holder.energy += plus
        return plus
    }
    
    override fun extractEnergy(energy: Long): Long {
        val minus = min(holder.energy, energy)
        holder.energy -= minus
        return minus
    }
    
    override fun getMaxEnergyInput(): Long {
        return Long.MAX_VALUE
    }
    
    override fun getMaxEnergyOutput(): Long {
        return Long.MAX_VALUE
    }
    
    override fun getConsumerBlockFaces(): MutableList<BlockFace> {
        return holder.connectionConfig.mapNotNullTo(ArrayList()) { if (it.value == NetworkConnectionType.INSERT) it.key else null }
    }
    
    override fun getProviderBlockFaces(): MutableList<BlockFace> {
        return holder.connectionConfig.mapNotNullTo(ArrayList()) { if (it.value == NetworkConnectionType.EXTRACT) it.key else null }
    }
    
}