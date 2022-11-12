package xyz.xenondevs.nova.data.world.block.state

import io.github.unifiedtechpower.unified.energy.manager.UnifiedEnergy
import io.github.unifiedtechpower.unified.energy.storage.EnergyStorage
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.buffer.ByteBuffer
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.integration.utp.UTPNetworkEndPoint
import xyz.xenondevs.nova.world.BlockPos
import java.util.*
import java.util.concurrent.CompletableFuture

internal class UTPBlockState(
    override val pos: BlockPos,
    val energyStorage: CompletableFuture<EnergyStorage?> = UnifiedEnergy.getInstance().getEnergyStorageAt(pos.location)
) : BlockState {
    
    override val id = ID
    
    @Volatile
    lateinit var data: Compound
        private set
    
    @Volatile
    lateinit var uuid: UUID
        private set
    
    @Volatile
    private var _utpEndPoint: UTPNetworkEndPoint? = null
    
    val utpEndPoint: UTPNetworkEndPoint
        get() = _utpEndPoint ?: throw IllegalStateException("UTPNetworkEndPoint is not initialized")
    
    override var isLoaded = false
        private set
    
    override fun handleInitialized(placed: Boolean) {
        if (!::data.isInitialized) data = Compound()
        if (!::uuid.isInitialized) uuid = UUID.randomUUID()
    
        _utpEndPoint = UTPNetworkEndPoint(this)
        utpEndPoint.handleInitialized(placed)
        
        isLoaded = true
    }
    
    override fun handleRemoved(broken: Boolean) {
        isLoaded = false
        if (_utpEndPoint != null) {
            utpEndPoint.saveData()
            utpEndPoint.handleRemoved(broken)
            _utpEndPoint = null
        }
    }
    
    override fun read(buf: ByteBuffer) {
        uuid = CBF.read(buf)!!
        data = CBF.read(buf)!!
    }
    
    override fun write(buf: ByteBuffer) {
        CBF.write(uuid, buf)
        
        if (_utpEndPoint != null)
            utpEndPoint.saveData()
        
        CBF.write(data, buf)
    }
    
    override fun toString(): String {
        return "UTPBlockState(pos=$pos, data=$data)"
    }
    
    companion object {
        val ID = NamespacedId("utp", "utp")
    }
    
}