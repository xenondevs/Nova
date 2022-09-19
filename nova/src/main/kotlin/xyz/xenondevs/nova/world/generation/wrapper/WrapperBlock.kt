package xyz.xenondevs.nova.world.generation.wrapper

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour

class WrapperBlock(val delegate: Block): Block(buildProperties(delegate)) {
    
    companion object {
        private fun buildProperties(block: Block): BlockBehaviour.Properties {
            TODO()
        }
    }
    
}