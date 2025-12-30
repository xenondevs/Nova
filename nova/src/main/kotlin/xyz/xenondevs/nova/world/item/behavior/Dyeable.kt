package xyz.xenondevs.nova.world.item.behavior

import io.papermc.paper.datacomponent.DataComponentTypes
import net.minecraft.tags.ItemTags
import net.minecraft.world.level.block.LayeredCauldronBlock
import org.bukkit.Statistic
import org.bukkit.block.Block
import org.bukkit.block.BlockType
import org.bukkit.entity.Player
import org.bukkit.event.block.CauldronLevelChangeEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.nmsEntity
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.item.vanilla.VanillaMaterialProperty

/**
 * Makes items dyeable.
 */
object Dyeable : ItemBehavior {
    
    override val vanillaMaterialProperties = provider(listOf(VanillaMaterialProperty.DYEABLE))
    
    override fun useOnBlock(itemStack: ItemStack, block: Block, ctx: Context<BlockInteract>): InteractionResult {
        val clickedPos = ctx[BlockInteract.BLOCK_POS]
        val entity = ctx[BlockInteract.SOURCE_ENTITY] ?: return InteractionResult.Pass
        val blockType = ctx[BlockInteract.BLOCK_TYPE_VANILLA] ?: return InteractionResult.Pass
        if (
            blockType == BlockType.CAULDRON
            && itemStack.hasData(DataComponentTypes.DYED_COLOR)
            && LayeredCauldronBlock.lowerFillLevel(
                clickedPos.nmsBlockState,
                clickedPos.world.serverLevel,
                clickedPos.nmsPos,
                entity.nmsEntity,
                CauldronLevelChangeEvent.ChangeReason.ARMOR_WASH
            )
        ) {
            itemStack.unsetData(DataComponentTypes.DYED_COLOR)
            (entity as? Player)?.incrementStatistic(Statistic.ARMOR_CLEANED)
            return InteractionResult.Success()
        }
        
        return InteractionResult.Pass
    }
    
    /**
     * Checks whether the given [itemStack] is dyeable, regardless of whether it is a Nova item or not.
     */
    @JvmStatic
    internal fun isDyeable(itemStack: net.minecraft.world.item.ItemStack): Boolean {
        return itemStack.novaItem?.hasBehavior<Dyeable>() ?: itemStack.`is`(ItemTags.DYEABLE)
    }
    
}