package xyz.xenondevs.nova.data.world.block.state

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundDeserializer
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntity
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.world.BlockPos

class VanillaTileEntityState(override val pos: BlockPos, override val id: String) : BlockState {
    
    lateinit var data: CompoundElement
    lateinit var tileEntity: VanillaTileEntity
    
    override fun handleInitialized(placed: Boolean) {
        if (!::data.isInitialized) data = CompoundElement()
        tileEntity = VanillaTileEntity.of(this)!!
        tileEntity.handleInitialized()
        VanillaTileEntityManager.registerTileEntity(this)
    }
    
    override fun handleRemoved(broken: Boolean) {
        tileEntity.handleRemoved(!broken)
        VanillaTileEntityManager.unregisterTileEntity(this)
    }
    
    override fun read(buf: ByteBuf) {
        data = CompoundDeserializer.read(buf)
    }
    
    override fun write(buf: ByteBuf) {
        tileEntity.saveData()
        data.write(buf)
    }
    
    override fun toString(): String {
        return "VanillaTileEntityState(pos=$pos, id=$id, data=$data)"
    }
    
}