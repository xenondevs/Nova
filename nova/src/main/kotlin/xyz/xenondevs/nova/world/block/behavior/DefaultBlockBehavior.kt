package xyz.xenondevs.nova.world.block.behavior

import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.world.block.DefaultBlockTags

internal class DefaultBlockBehavior : BlockBehavior {
    
    override val tags = provider(setOf(DefaultBlockTags.NOVA))
    
}