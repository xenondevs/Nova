package xyz.xenondevs.nova.world.block.behavior

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.collections.firstInstanceOfOrNull
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.util.addToInventoryOrDrop
import xyz.xenondevs.nova.util.addToInventoryPrioritizedOrDrop
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.FluidType
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.container.NetworkedFluidContainer
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.holder.FluidHolder
import xyz.xenondevs.nova.world.format.WorldDataManager

/**
 * Allows filling and emptying fluid containers of [TileEntities][TileEntity]
 * that implement [NetworkEndPoint] and have a [FluidHolder] with buckets.
 */
object Bucketable : BlockBehavior {
    
    override fun useItemOn(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockInteract>): InteractionResult {
        val player = ctx[BlockInteract.SOURCE_PLAYER]
            ?: return InteractionResult.Pass
        val hand = ctx[BlockInteract.HELD_HAND]
            ?: return InteractionResult.Pass
        val item = player.inventory.getItem(hand).takeUnlessEmpty()
            ?: return InteractionResult.Pass
        val tileEntity = WorldDataManager.getTileEntity(pos) as? NetworkEndPoint
            ?: return InteractionResult.Pass
        val fluidHolder = tileEntity.holders.firstInstanceOfOrNull<FluidHolder>()
            ?: return InteractionResult.Pass
        val clickedFace = ctx[BlockInteract.CLICKED_BLOCK_FACE]
        
        if (item.type == Material.BUCKET) {
            // move fluid from tile-entity to bucket
            val container = selectContainerExtract(fluidHolder, clickedFace)
                ?: return InteractionResult.Pass
            val fluidType = container.type!!
            container.takeFluid(1000)
            if (player.gameMode != GameMode.CREATIVE)
                fillBucketInHand(player, hand, fluidType)
            val sound = when (container.type) {
                FluidType.LAVA -> Sound.ITEM_BUCKET_FILL_LAVA
                else -> Sound.ITEM_BUCKET_FILL
            }
            pos.playSound(sound, 1f, 1f)
        } else {
            val fluidType = FluidType.entries.firstOrNull { it.bucket.type == item.type }
                ?: return InteractionResult.Pass
            
            // move fluid from bucket to tile-entity
            val container = selectContainerInsert(fluidHolder, fluidType, clickedFace)
                ?: return InteractionResult.Pass
            container.addFluid(fluidType, 1000)
            if (player.gameMode != GameMode.CREATIVE)
                emptyBucketInHand(player, hand)
            val sound = when (fluidType) {
                FluidType.LAVA -> Sound.ITEM_BUCKET_EMPTY_LAVA
                else -> Sound.ITEM_BUCKET_EMPTY
            }
            pos.playSound(sound, 1f, 1f)
        }
        
        return InteractionResult.Success(swing = true)
    }
    
    private fun selectContainerInsert(fluidHolder: FluidHolder, fluidType: FluidType, clickedFace: BlockFace?): NetworkedFluidContainer? {
        if (clickedFace != null)
            return selectContainerStrictInsert(fluidHolder, fluidType, clickedFace) ?: selectContainerInsert(fluidHolder, fluidType)
        return selectContainerInsert(fluidHolder, fluidType)
    }
    
    private fun selectContainerExtract(fluidHolder: FluidHolder, clickedFace: BlockFace?): NetworkedFluidContainer? {
        if (clickedFace != null)
            return selectContainerStrictExtract(fluidHolder, clickedFace) ?: selectContainerExtract(fluidHolder)
        return selectContainerExtract(fluidHolder)
    }
    
    private fun selectContainerStrictInsert(fluidHolder: FluidHolder, fluidType: FluidType, clickedFace: BlockFace): NetworkedFluidContainer? {
        val container = fluidHolder.containerConfig[clickedFace]
            ?: return null
        val conType = fluidHolder.containers[container]
            ?: return null
        if (!conType.insert)
            return null
        if (!container.accepts(fluidType, 1000))
            return null
        
        return container
    }
    
    private fun selectContainerStrictExtract(fluidHolder: FluidHolder, clickedFace: BlockFace): NetworkedFluidContainer? {
        val container = fluidHolder.containerConfig[clickedFace]
            ?: return null
        val conType = fluidHolder.containers[container]
            ?: return null
        if (!conType.extract)
            return null
        if (container.type == null || container.amount < 1000)
            return null
        
        return container
    }
    
    private fun selectContainerInsert(fluidHolder: FluidHolder, fluidType: FluidType): NetworkedFluidContainer? {
        for ((container, conType) in fluidHolder.containers.entries) {
            if (!conType.insert)
                continue
            if (!container.accepts(fluidType, 1000))
                continue
            
            return container
        }
        
        return null
    }
    
    private fun selectContainerExtract(fluidHolder: FluidHolder): NetworkedFluidContainer? {
        for ((container, conType) in fluidHolder.containers.entries) {
            if (!conType.extract)
                continue
            if (container.type == null || container.amount < 1000)
                continue
            
            return container
        }
        
        return null
    }
    
    internal fun emptyBucketInHand(player: Player, hand: EquipmentSlot) {
        val itemStack = player.inventory.getItem(hand)
        val bucket = ItemStack(Material.BUCKET)
        if (itemStack.amount > 1) {
            itemStack.amount--
            player.addToInventoryOrDrop(bucket)
        } else {
            player.inventory.setItem(hand, bucket)
        }
    }
    
    internal fun fillBucketInHand(player: Player, hand: EquipmentSlot, fluidType: FluidType) {
        val bucket = fluidType.bucket
        player.inventory.getItem(hand).amount--
        player.addToInventoryPrioritizedOrDrop(hand, bucket)
    }
    
}