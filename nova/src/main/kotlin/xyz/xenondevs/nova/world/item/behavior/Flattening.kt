package xyz.xenondevs.nova.world.item.behavior

import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.component.BlockTransformers
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.util.REGISTRY_ACCESS
import xyz.xenondevs.nova.world.item.DataComponentMap
import xyz.xenondevs.nova.world.item.buildDataComponentMapProvider

/**
 * Allows items to flatten the ground.
 */
object Flattening : ItemBehavior {
    
    override val baseDataComponents: Provider<DataComponentMap> = buildDataComponentMapProvider {
        this[DataComponents.BLOCK_TRANSFORMER] = REGISTRY_ACCESS.lookupOrThrow(Registries.BLOCK_TRANSFORMER).getOrThrow(BlockTransformers.SHOVEL)
    }
    
}
