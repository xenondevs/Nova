package xyz.xenondevs.nova.world.item.behavior

import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.component.BlockTransformers
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.util.REGISTRY_ACCESS
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.item.DataComponentMap
import xyz.xenondevs.nova.world.item.buildDataComponentMapProvider

/**
 * Allows items to till the ground.
 */
object Tilling : ItemBehavior {
    
    override val baseDataComponents: Provider<DataComponentMap> = buildDataComponentMapProvider {
        this[DataComponents.BLOCK_TRANSFORMER] = REGISTRY_ACCESS.lookupOrThrow(Registries.BLOCK_TRANSFORMER).getOrThrow(BlockTransformers.HOE)
    }
    
    override fun useOnBlock(itemStack: ItemStack, block: Block, ctx: Context<BlockInteract>): InteractionResult =
        useBlockTransformer(itemStack, block, ctx)
    
}
