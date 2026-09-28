package xyz.xenondevs.nova.world.item

import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.NovaRegistrar.item
import xyz.xenondevs.nova.registry.RegistryLoader
import xyz.xenondevs.nova.world.item.behavior.UnknownItemFilterBehavior

@InternalInit(
    stage = InternalInitStage.PRE_WORLD,
    runBefore = [RegistryLoader::class]
)
internal object DefaultItems {
    
    val UNKNOWN_ITEM_FILTER = item("unknown_item_filter") {
        behaviors(UnknownItemFilterBehavior)
        modelDefinition { model = buildModel { createLayeredModel("block/unknown") } }
        hidden(true)
    }
    
}