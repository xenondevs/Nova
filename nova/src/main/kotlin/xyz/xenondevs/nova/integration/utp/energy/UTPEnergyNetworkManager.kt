package xyz.xenondevs.nova.integration.utp.energy

import io.github.unifiedtechpower.unified.energy.manager.EnergyNetworkManager
import io.github.unifiedtechpower.unified.energy.manager.UnifiedEnergy
import io.github.unifiedtechpower.unified.energy.storage.EnergyStorage
import io.github.unifiedtechpower.unified.energy.unit.EnergyUnit
import org.bukkit.Chunk
import org.bukkit.Location
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.network.energy.holder.BufferEnergyHolder
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.network.energy.holder.NovaEnergyHolder
import xyz.xenondevs.nova.tileentity.network.energy.holder.ProviderEnergyHolder
import xyz.xenondevs.nova.world.chunkPos
import xyz.xenondevs.nova.world.pos
import java.util.concurrent.CompletableFuture

internal object UTPEnergyNetworkManager : EnergyNetworkManager {
    
    val ENERGY_UNIT = EnergyUnit.getDefault()
    
    fun init() {
        UnifiedEnergy.getInstance().addManager(this)
    }
    
    override fun getEnergyStorageAt(location: Location): CompletableFuture<EnergyStorage?> {
        val future = CompletableFuture<EnergyStorage?>()
        
        val pos = location.chunkPos
        WorldDataManager.runAfterChunkLoad(pos) {
            var energyStorage: EnergyStorage? = null
            
            val tileEntity = TileEntityManager.getTileEntity(location) as? NetworkedTileEntity
            if (tileEntity != null) {
                val energyHolder = tileEntity.holders[NetworkType.ENERGY] as? NovaEnergyHolder
                if (energyHolder != null) {
                    energyStorage = wrapEnergyHolder(energyHolder)
                }
            }
            
            future.complete(energyStorage)
        }
        
        return future
    }
    
    override fun getEnergyStoragesIn(chunk: Chunk): CompletableFuture<MutableMap<Location, EnergyStorage>> {
        val future = CompletableFuture<MutableMap<Location, EnergyStorage>>()
        
        val pos = chunk.pos
        WorldDataManager.runAfterChunkLoad(pos) {
            val storages = TileEntityManager.getTileEntitiesInChunk(pos).asSequence()
                .filterIsInstance<NetworkedTileEntity>()
                .mapNotNull { it.holders[NetworkType.ENERGY] as? NovaEnergyHolder }
                .associateTo(HashMap()) { it.endPoint.location to wrapEnergyHolder(it) }
            
            future.complete(storages)
        }
        
        return future
    }
    
    private fun wrapEnergyHolder(holder: NovaEnergyHolder): EnergyStorage {
        return when (holder) {
            is ProviderEnergyHolder -> NovaEnergyProviderWrapper(holder)
            is ConsumerEnergyHolder -> NovaEnergyConsumerWrapper(holder)
            is BufferEnergyHolder -> NovaEnergyBufferWrapper(holder)
        }
    }
    
}