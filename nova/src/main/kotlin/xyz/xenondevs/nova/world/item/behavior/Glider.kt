package xyz.xenondevs.nova.world.item.behavior

import io.papermc.paper.datacomponent.DataComponentTypes
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.world.item.DataComponentMap
import xyz.xenondevs.nova.world.item.buildDataComponentMapProvider

/**
 * Allows elytra-like gliding if equipped.
 */
object Glider : ItemBehavior {
    
    override val baseDataComponents: Provider<DataComponentMap> = buildDataComponentMapProvider { 
        set(DataComponentTypes.GLIDER) 
    }
    
}