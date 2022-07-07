package xyz.xenondevs.nova.data.world.block.property

import io.netty.buffer.ByteBuf
import org.bukkit.Location
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.util.BlockFaceUtils
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext

class Directional(private val upDown: Boolean = false) : BlockProperty {
    
    lateinit var facing: BlockFace
    
    override fun init(ctx: BlockPlaceContext) {
        facing = ctx.sourceLocation?.let(::getFacing) ?: BlockFace.NORTH
        println(facing)
    }
    
    override fun read(buf: ByteBuf) {
        facing = BlockFace.values()[buf.readByte().toInt()]
    }
    
    override fun write(buf: ByteBuf) {
        buf.writeByte(if (::facing.isInitialized) facing.ordinal else 0)
    }
    
    private fun getFacing(loc: Location): BlockFace {
        if (upDown) {
            if (loc.pitch < -45)
                return BlockFace.DOWN
            if (loc.pitch > 45)
                return BlockFace.UP
        }
        
        return BlockFaceUtils.getDirection(loc.yaw + 180)
    }
    
    companion object {
        
        val NORMAL = object : BlockPropertyType<Directional> {
            override fun create() = Directional(false)
        }
        
        val ALL = object : BlockPropertyType<Directional> {
            override fun create() = Directional(true)
        }
        
    }
    
}