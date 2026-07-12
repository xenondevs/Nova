package xyz.xenondevs.nova.world.block

import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.NovaRegistrar.block
import xyz.xenondevs.nova.registry.RegistryLoader
import xyz.xenondevs.nova.resources.builder.layout.block.BackingStateCategory
import xyz.xenondevs.nova.world.block.behavior.UnknownBlockBehavior

@InternalInit(
    stage = InternalInitStage.PRE_WORLD,
    runBefore = [RegistryLoader::class]
)
internal object DefaultBlocks {
    
    val UNKNOWN by block("unknown") {
        behaviors(UnknownBlockBehavior)
        stateBacked(Int.MAX_VALUE, BackingStateCategory.NOTE_BLOCK, BackingStateCategory.MUSHROOM_BLOCK)
    }
    
}