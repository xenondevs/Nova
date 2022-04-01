package xyz.xenondevs.nova.data.world.block.property

import io.netty.buffer.ByteBuf
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.util.BlockFaceUtils
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext

class Directional : BlockProperty {
    
    lateinit var facing: BlockFace
    
    override fun init(ctx: BlockPlaceContext) {
        facing = BlockFaceUtils.getDirection((ctx.sourceLocation?.yaw ?: 0F) + 180)
    }
    
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