package xyz.xenondevs.nova.data.world.block.property

import io.netty.buffer.ByteBuf
import org.bukkit.block.BlockFace

class Directional : BlockProperty {
    
    lateinit var facing: BlockFace
    
    override fun read(buf: ByteBuf) {
        facing = BlockFace.values()[buf.readByte().toInt()]
    }
    
    override fun write(buf: ByteBuf) {
        buf.writeByte(if (::facing.isInitialized) facing.ordinal else 0)
    }
    
    companion object : BlockPropertyType<Directional> {
        override fun create() = Directional()
    }
    
}