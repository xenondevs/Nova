package xyz.xenondevs.nova.world.block.backingstate.impl

import net.minecraft.util.Mth
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.DaylightDetectorBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.gameevent.GameEvent
import org.bukkit.craftbukkit.v1_19_R3.event.CraftEventFactory
import xyz.xenondevs.nova.data.resources.model.blockstate.DaylightDetectorBlockStateConfig
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.data.world.block.state.LinkedBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.tileentity.vanilla.VanillaDaylightDetectorTileEntity
import xyz.xenondevs.nova.world.BlockLocation
import xyz.xenondevs.nova.world.block.backingstate.BackingState
import xyz.xenondevs.nova.world.block.model.BlockStateBlockModelProvider
import kotlin.math.round
import net.minecraft.core.BlockPos as MojangBlockPos

internal object DaylightDetectorBackingState : BackingState(DaylightDetectorBlockStateConfig, false) {
    
    fun updatePower(vdd: VanillaDaylightDetectorTileEntity, world: Level, pos: MojangBlockPos, state: BlockState) {
        // Convert sun angle to value between 0 and 15
        var brightness = world.getBrightness(LightLayer.SKY, pos) - world.skyDarken
        var sunAngle = world.getSunAngle(1.0f)
        if (state.getValue(DaylightDetectorBlock.INVERTED)) {
            brightness = 15 - brightness
        } else if (brightness > 0) {
            val angleOffset = if (sunAngle < Math.PI) 0.0 else Math.PI * 2
            sunAngle += ((angleOffset - sunAngle) * 0.2).toFloat()
            brightness = round(brightness * Mth.cos(sunAngle)).toInt()
        }
        val clampedBrightness = Mth.clamp(brightness, 0, 15)
        
        val currentPower = vdd.power
        if (currentPower != clampedBrightness) {
            val redstoneEvent = CraftEventFactory.callRedstoneChange(world, pos, currentPower, clampedBrightness)
            val newPower: Int = redstoneEvent.newCurrent
            vdd.power = newPower
            world.updateNeighborsAt(pos, Blocks.DAYLIGHT_DETECTOR)
        }
    }
    
    fun cycleInverted(vdd: VanillaDaylightDetectorTileEntity, world: Level, pos: MojangBlockPos, state: BlockState, player: Player): InteractionResult {
        if (!player.mayBuild())
            return InteractionResult.PASS
        if (world.isClientSide)
            return InteractionResult.SUCCESS
        
        val newState = state.cycle(DaylightDetectorBlock.INVERTED)
        world.setBlock(pos, newState, Block.UPDATE_NONE)
        world.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, newState))
        updatePower(vdd, world, pos, newState)
        return InteractionResult.CONSUME
    }
    
    override fun getCorrectBlockState(pos: BlockLocation): BlockState? {
        var state = WorldDataManager.getBlockState(pos)
        
        if (state is LinkedBlockState)
            state = state.blockState
        
        if (state is NovaBlockState)
            return (state.modelProvider as? BlockStateBlockModelProvider)?.currentBlockState
        
        return pos.nmsBlockState.setValue(DaylightDetectorBlock.POWER, 0)
    }
    
}