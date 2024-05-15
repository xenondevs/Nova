package xyz.xenondevs.nova.world.block.behavior

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.collections.firstInstanceOfOrNull
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.DefaultContextIntentions.BlockInteract
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.type.fluid.FluidType
import xyz.xenondevs.nova.tileentity.network.type.fluid.container.NetworkedFluidContainer
import xyz.xenondevs.nova.tileentity.network.type.fluid.holder.FluidHolder
import xyz.xenondevs.nova.util.addItemCorrectly
import xyz.xenondevs.nova.util.dropItem
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.format.WorldDataManager

/**
 * Allows filling [TileEntities][TileEntity] that implement [NetworkEndPoint] and
 * have a [FluidHolder] with fluids by right-clicking them with a bucket.
 */
object FluidFillable : BlockBehavior {
    
    override fun handleInteract(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockInteract>): Boolean {
        val player = ctx[DefaultContextParamTypes.SOURCE_PLAYER]
            ?: return false
        val hand = ctx[DefaultContextParamTypes.INTERACTION_HAND]
            ?: return false
        val item = player.inventory.getItem(hand).takeUnlessEmpty()
            ?: return false
        val fluidType = FluidType.entries.firstOrNull { it.bucket.type == item.type }
            ?: return false
        val tileEntity = WorldDataManager.getTileEntity(pos) as? NetworkEndPoint
            ?: return false
        val fluidHolder = tileEntity.holders.firstInstanceOfOrNull<FluidHolder>()
            ?: return false
        val clickedFace = ctx[DefaultContextParamTypes.CLICKED_BLOCK_FACE]
        
        val container = selectContainer(fluidHolder, fluidType, clickedFace)
        if (container != null && container.accepts(fluidType, 1000L)) {
            container.addFluid(fluidType, 1000)
            if (player.gameMode != GameMode.CREATIVE)
                emptyBucketInHand(player, hand)
            return true
        }
        
        return false
    }
    
    private fun selectContainer(fluidHolder: FluidHolder, fluidType: FluidType, clickedFace: BlockFace?): NetworkedFluidContainer? {
        if (clickedFace != null) {
            return fluidHolder.containerConfig[clickedFace]
                ?.takeUnless { fluidHolder.containers[it]?.insert != true }
        } else {
            return fluidHolder.containers.entries
                .firstOrNull { (container, conType) -> conType.insert && (container.type == null || container.type == fluidType) }
                ?.key
        }
    }
    
    private fun emptyBucketInHand(player: Player, hand: EquipmentSlot) {
        val itemStack = player.inventory.getItem(hand)
        val bucket = ItemStack(Material.BUCKET)
        if (itemStack.amount > 1) {
            itemStack.amount--
            if (player.inventory.addItemCorrectly(bucket) > 0) {
                player.location.dropItem(bucket)
            }
        } else {
            player.inventory.setItem(hand, bucket)
        }
    }
    
}