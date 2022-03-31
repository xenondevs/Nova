package xyz.xenondevs.nova.data.world.block.state

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundDeserializer
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.world.BlockPos

class VanillaTileState(override val pos: BlockPos, override val id: String) : BlockState {
    
    lateinit var compound: CompoundElement
    
    override fun handleInitialized(placed: Boolean) = Unit
    
    override fun handleRemoved(broken: Boolean) = Unit
    
    override fun read(buf: ByteBuf) {
        if (buf.readByte().toInt() == 1) {
            compound = CompoundDeserializer.read(buf)
        }
    }
    
    override fun write(buf: ByteBuf) {
        if (::compound.isInitialized) {
            buf.writeByte(1)
            compound.write(buf)
        } else buf.writeByte(0)
    }
    
}