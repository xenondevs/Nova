package xyz.xenondevs.nova.data.world.block.property

import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext

/**
 * Removes the legacy directional property without giving the block new directional properties.
 */
class LegacyDirectional : BlockProperty {
    override fun init(ctx: BlockPlaceContext) = Unit
    override fun read(compound: Compound) = Unit
    override fun write(compound: Compound) = Unit
    
    companion object : BlockPropertyType<LegacyDirectional> {
        override fun create() = LegacyDirectional()
    }
}