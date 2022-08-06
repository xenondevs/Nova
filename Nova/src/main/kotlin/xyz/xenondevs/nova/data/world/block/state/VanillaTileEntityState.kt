package xyz.xenondevs.nova.data.world.block.state

import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.buffer.ByteBuffer
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.LegacyCompound
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntity
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.world.BlockPos

internal class VanillaTileEntityState(override val pos: BlockPos, override val id: NamespacedId) : BlockState {
    
    lateinit var data: Compound
    lateinit var tileEntity: VanillaTileEntity
    var legacyData: LegacyCompound? = null
    
    constructor(pos: BlockPos, id: String) : this(pos, NamespacedId.of(id))
    
    override fun handleInitialized(placed: Boolean) {
        if (!::data.isInitialized) data = Compound()
        tileEntity = VanillaTileEntity.of(this)!!
        tileEntity.handleInitialized()
        VanillaTileEntityManager.registerTileEntity(this)
    }
    
    override fun handleRemoved(broken: Boolean) {
        // The tile entity could be null when the chunk was unloaded before the WorldDataManager could call handleInitialized
        if (::tileEntity.isInitialized) {
            tileEntity.handleRemoved(!broken)
            VanillaTileEntityManager.unregisterTileEntity(this)
        }
    }
    
    override fun read(buf: ByteBuffer) {
        data = CBF.read(buf)!!
    }
    
    override fun write(buf: ByteBuffer) {
        tileEntity.saveData()
        CBF.write(data, buf)
    }
    
    override fun toString(): String {
        return "VanillaTileEntityState(pos=$pos, id=$id, data=$data)"
    }
    
}