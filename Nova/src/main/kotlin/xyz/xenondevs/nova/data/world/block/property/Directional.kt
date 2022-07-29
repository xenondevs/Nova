package xyz.xenondevs.nova.data.world.block.property

import org.bukkit.Location
import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.util.BlockFaceUtils
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext

class Directional(private val upDown: Boolean = false) : BlockProperty {
    
    lateinit var facing: BlockFace
    
    override fun init(ctx: BlockPlaceContext) {
        facing = ctx.sourceLocation?.let(::getFacing) ?: BlockFace.NORTH
    }
    
    override fun read(compound: Compound) {
        facing = compound["facing"]!!
    }
    
    override fun write(compound: Compound) {
        compound["facing"] = facing
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