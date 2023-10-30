package xyz.xenondevs.nova.data.world.block.property

import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockPlace
import xyz.xenondevs.nova.data.context.param.ContextParamTypes
import xyz.xenondevs.nova.util.BlockFaceUtils
import xyz.xenondevs.nova.util.calculateYawPitch

class Directional(private val upDown: Boolean = false) : BlockProperty {
    
    lateinit var facing: BlockFace
    
    override fun init(ctx: Context<BlockPlace>) {
        val blockFacing: BlockFace? = ctx[ContextParamTypes.BLOCK_FACING]
        if (blockFacing != null) {
            this.facing = blockFacing
            return
        }
        
        val sourceDirection = ctx[ContextParamTypes.SOURCE_DIRECTION]
        if (sourceDirection != null) {
            val (yaw, pitch) = sourceDirection.calculateYawPitch()
            this.facing = getFacing(yaw, pitch)
            return
        }
        
        this.facing = BlockFace.NORTH
    }
    
    override fun read(compound: Compound) {
        facing = compound["facing"]!!
    }
    
    override fun write(compound: Compound) {
        compound["facing"] = facing
    }
    
    private fun getFacing(yaw: Float, pitch: Float): BlockFace {
        if (upDown) {
            if (pitch < -45)
                return BlockFace.DOWN
            if (pitch > 45)
                return BlockFace.UP
        }
        
        return BlockFaceUtils.getDirection(yaw + 180)
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