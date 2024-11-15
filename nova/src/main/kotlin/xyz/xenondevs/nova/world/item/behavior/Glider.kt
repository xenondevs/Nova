package xyz.xenondevs.nova.world.item.behavior

import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.util.Unit
import xyz.xenondevs.commons.provider.provider

/**
 * Allows elytra-like gliding if equipped.
 */
object Glider : ItemBehavior {
    
    override val baseDataComponents = provider {
        DataComponentMap.builder()
            .set(DataComponents.GLIDER, Unit.INSTANCE)
            .build()
    }
    
}