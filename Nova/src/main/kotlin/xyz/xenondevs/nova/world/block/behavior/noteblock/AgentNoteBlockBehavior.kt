package xyz.xenondevs.nova.world.block.behavior.noteblock

import net.minecraft.core.BlockPos
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.nova.tileentity.vanilla.VanillaNoteBlockTileEntity
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.util.toNovaPos

internal object AgentNoteBlockBehavior {
    
    @JvmStatic
    fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, block: Block, neighborPos: BlockPos, flag: Boolean) {
        val vnb = VanillaTileEntityManager.getTileEntityAt(pos.toNovaPos(level.world)) as? VanillaNoteBlockTileEntity ?: return
        
        val shouldBePowered = level.hasNeighborSignal(pos)
        if (shouldBePowered != vnb.powered) {
            vnb.powered = shouldBePowered
            
            if (shouldBePowered)
                NoteBlockBehavior.playNote(vnb)
        }
    }
    
    @JvmStatic
    fun use(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, result: BlockHitResult): InteractionResult {
        val vnb = VanillaTileEntityManager.getTileEntityAt(pos.toNovaPos(level.world)) as? VanillaNoteBlockTileEntity ?: return InteractionResult.FAIL
        
        NoteBlockBehavior.cycleNote(vnb)
        player.awardStat(Stats.TUNE_NOTEBLOCK)
        
        return InteractionResult.CONSUME
    }
    
    @JvmStatic
    fun attack(state: BlockState, level: Level, pos: BlockPos, player: Player) {
        val vnb = VanillaTileEntityManager.getTileEntityAt(pos.toNovaPos(level.world)) as? VanillaNoteBlockTileEntity ?: return
        
        NoteBlockBehavior.playNote(vnb)
        player.awardStat(Stats.PLAY_NOTEBLOCK)
    }
    
    @JvmStatic
    fun triggerEvent(state: BlockState, level: Level, pos: BlockPos, i: Int, j: Int) = true
    
}