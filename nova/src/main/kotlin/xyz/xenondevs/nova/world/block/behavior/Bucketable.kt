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
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions.BlockInteract
import xyz.xenondevs.nova.context.param.DefaultContextParamTypes
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.FluidType
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.container.NetworkedFluidContainer
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.holder.FluidHolder
import xyz.xenondevs.nova.util.addItemCorrectly
import xyz.xenondevs.nova.util.addPrioritized
import xyz.xenondevs.nova.util.dropItem
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.format.WorldDataManager

/**
 * Allows filling and emptying fluid containers of [TileEntities][TileEntity]
 * that implement [NetworkEndPoint] and have a [FluidHolder] with buckets.
 */
object Bucketable : BlockBehavior {
    
    override fun handleInteract(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockInteract>): Boolean {
        val player = ctx[DefaultContextParamTypes.SOURCE_PLAYER]
            ?: return false
        val hand = ctx[DefaultContextParamTypes.INTERACTION_HAND]
            ?: return false
        val item = player.inventory.getItem(hand).takeUnlessEmpty()
            ?: return false
        val tileEntity = WorldDataManager.getTileEntity(pos) as? NetworkEndPoint
            ?: return false
        val fluidHolder = tileEntity.holders.firstInstanceOfOrNull<FluidHolder>()
            ?: return false
        val clickedFace = ctx[DefaultContextParamTypes.CLICKED_BLOCK_FACE]
        
        if (item.type == Material.BUCKET) {
            // move fluid from tile-entity to bucket
            val container = selectContainerExtract(fluidHolder, clickedFace)
                ?: return false
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
                ?: return false
            
            // move fluid from bucket to tile-entity
            val container = selectContainerInsert(fluidHolder, fluidType, clickedFace)
                ?: return false
            container.addFluid(fluidType, 1000)
            if (player.gameMode != GameMode.CREATIVE)
                emptyBucketInHand(player, hand)
            val sound = when (fluidType) {
                FluidType.LAVA -> Sound.ITEM_BUCKET_EMPTY_LAVA
                else -> Sound.ITEM_BUCKET_EMPTY
            }
            pos.playSound(sound, 1f, 1f)
        }
        
        player.swingHand(hand)
        return true
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
    
    private fun fillBucketInHand(player: Player, hand: EquipmentSlot, fluidType: FluidType) {
        val bucket = fluidType.bucket
        player.inventory.getItem(hand).amount--
        player.inventory.addPrioritized(hand, bucket)
    }
    
}