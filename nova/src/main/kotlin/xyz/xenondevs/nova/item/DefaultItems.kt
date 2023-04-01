package xyz.xenondevs.nova.item

import xyz.xenondevs.nova.api.NovaMaterialRegistry
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.item.behavior.impl.WrenchBehavior

@InternalInit(stage = InitializationStage.PRE_WORLD)
object DefaultItems {
    
    val WRENCH = NovaMaterialRegistry.registerCoreItem("wrench", WrenchBehavior)
    
}