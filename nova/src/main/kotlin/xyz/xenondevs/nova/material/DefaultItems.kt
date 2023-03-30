package xyz.xenondevs.nova.material

import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.item.impl.WrenchBehavior

@InternalInit(stage = InitializationStage.PRE_WORLD)
object DefaultItems {
    
    val WRENCH = NovaMaterialRegistry.registerCoreItem("wrench", WrenchBehavior)
    
    fun init() = Unit
    
}