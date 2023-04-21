@file:Suppress("UNUSED_PARAMETER", "DEPRECATION")

package xyz.xenondevs.nova.transformer.patch.block

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.DaylightDetectorBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import xyz.xenondevs.nova.tileentity.vanilla.VanillaDaylightDetectorTileEntity
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.transformer.ClassTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.DAYLIGHT_DETECTOR_BLOCK_UPDATE_SIGNAL_STRENGTH_METHOD
import xyz.xenondevs.nova.util.toNovaPos
import xyz.xenondevs.nova.world.block.backingstate.impl.DaylightDetectorBackingState

internal object DaylightDetectorPatch : ClassTransformer(DaylightDetectorBlock::class) {
    
    
    override fun transform() {
        DaylightDetectorBlock::getSignal.replaceWith(::getSignal)
        DAYLIGHT_DETECTOR_BLOCK_UPDATE_SIGNAL_STRENGTH_METHOD.replaceWith(::updateSignalStrength)
        DaylightDetectorBlock::use.replaceWith(::use)
        DaylightDetectorBlock::isSignalSource.replaceWith(::isSignalSource)
    }
    
    @JvmStatic
    fun getSignal(thisRef: DaylightDetectorBlock, state: BlockState, world: BlockGetter, pos: BlockPos, direction: Direction): Int {
        if (world !is Level) return state.getValue(DaylightDetectorBlock.POWER)
        val vdd = VanillaTileEntityManager.getTileEntityAt(pos.toNovaPos(world.world)) as? VanillaDaylightDetectorTileEntity ?: return 0
        
        return vdd.power
    }
    
    @JvmStatic
    fun updateSignalStrength(state: BlockState, world: Level, pos: BlockPos) {
        val vdd = VanillaTileEntityManager.getTileEntityAt(pos.toNovaPos(world.world)) as? VanillaDaylightDetectorTileEntity ?: return
        
        DaylightDetectorBackingState.updatePower(vdd, world, pos, state)
    }
    
    @JvmStatic
    fun use(thisRef: DaylightDetectorBlock, state: BlockState, world: Level, pos: BlockPos, player: Player, hand: InteractionHand, result: BlockHitResult): InteractionResult {
        val vdd = VanillaTileEntityManager.getTileEntityAt(pos.toNovaPos(world.world)) as? VanillaDaylightDetectorTileEntity ?: return InteractionResult.FAIL

        return DaylightDetectorBackingState.cycleInverted(vdd, world, pos, state, player)
    }
    
    @JvmStatic
    fun isSignalSource(thisRef: DaylightDetectorBlock, state: BlockState): Boolean {
        return state.getValue(DaylightDetectorBlock.POWER) == 0
    }
    
}