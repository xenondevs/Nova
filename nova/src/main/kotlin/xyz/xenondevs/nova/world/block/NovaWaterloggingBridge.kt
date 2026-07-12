package xyz.xenondevs.nova.world.block

import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.BucketPickup
import net.minecraft.world.level.block.LiquidBlockContainer
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import java.util.Optional

internal object NovaWaterloggingBridge : LiquidBlockContainer, BucketPickup {
    
    @JvmStatic
    fun isWaterloggable(block: Any?): Boolean =
        block is NovaBlock && block.defaultBlockState.hasProperty(BlockStateProperties.WATERLOGGED)
    
    @JvmStatic
    fun scheduleWaterTickIfWaterlogged(state: BlockState, level: LevelAccessor, pos: BlockPos) {
        if (state.getOptionalValue(BlockStateProperties.WATERLOGGED).orElse(false) == true)
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level))
    }
    
    override fun canPlaceLiquid(
        user: LivingEntity?,
        level: BlockGetter,
        pos: BlockPos,
        state: BlockState,
        fluid: Fluid
    ): Boolean = fluid.isSame(Fluids.WATER) &&
        isWaterloggable(state.block) &&
        state.getOptionalValue(BlockStateProperties.WATERLOGGED).orElse(true) == false
    
    override fun placeLiquid(
        level: LevelAccessor,
        pos: BlockPos,
        state: BlockState,
        fluidState: FluidState
    ): Boolean {
        if (!canPlaceLiquid(null, level, pos, state, fluidState.type))
            return false
        
        if (!level.isClientSide) {
            val waterloggedState = state.setValue(BlockStateProperties.WATERLOGGED, true)
            level.setBlock(pos, waterloggedState, Block.UPDATE_ALL)
            scheduleWaterTickIfWaterlogged(waterloggedState, level, pos)
        }
        return true
    }
    
    override fun pickupBlock(
        user: LivingEntity?,
        level: LevelAccessor,
        pos: BlockPos,
        state: BlockState
    ): ItemStack {
        if (!isWaterloggable(state.block) || state.getOptionalValue(BlockStateProperties.WATERLOGGED).orElse(false) != true)
            return ItemStack.EMPTY
        
        level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, false), Block.UPDATE_ALL)
        if (!state.canSurvive(level, pos))
            level.destroyBlock(pos, true)
        return ItemStack(Items.WATER_BUCKET)
    }
    
    override fun getPickupSound(): Optional<SoundEvent> =
        Fluids.WATER.pickupSound
    
}
