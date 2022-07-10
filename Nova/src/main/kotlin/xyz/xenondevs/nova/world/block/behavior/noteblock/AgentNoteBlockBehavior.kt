package xyz.xenondevs.nova.world.block.behavior.noteblock

import net.minecraft.core.BlockPos
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.NoteBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.redefine
import xyz.xenondevs.nova.tileentity.vanilla.VanillaNoteBlockTileEntity
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.util.toNovaPos

internal object AgentNoteBlockBehavior {
    
    private const val BLOCK_STATE = "SRC/(net.minecraft.world.level.block.state.BlockState)"
    private const val LEVEL = "SRC/(net.minecraft.world.level.Level)"
    private const val BLOCK_POS = "SRC/(net.minecraft.core.BlockPos)"
    private const val BLOCK = "SRC/(net.minecraft.world.level.block.Block)"
    private const val PLAYER = "SRC/(net.minecraft.world.entity.player.Player)"
    private const val INTERACTION_HAND = "SRC/(net.minecraft.world.InteractionHand)"
    private const val BLOCK_HIT_RESULT = "SRC/(net.minecraft.world.phys.BlockHitResult)"
    private const val INTERACTION_RESULT = "SRC/(net.minecraft.world.InteractionResult)"
    
    fun init() {
        NoteBlock::class.redefine {
            getMethod(
                "SRM(net.minecraft.world.level.block.NoteBlock neighborChanged)",
                "(L$BLOCK_STATE;L$LEVEL;L$BLOCK_POS;L$BLOCK;L$BLOCK_POS;Z)V"
            )!!.instructions = VirtualClassPath.getInstructions(NoteBlockMethods::class, "neighborChanged")
            
            getMethod(
                "SRM(net.minecraft.world.level.block.NoteBlock use)",
                "(L$BLOCK_STATE;L$LEVEL;L$BLOCK_POS;L$PLAYER;L$INTERACTION_HAND;L$BLOCK_HIT_RESULT;)L${INTERACTION_RESULT};"
            )!!.instructions = VirtualClassPath.getInstructions(NoteBlockMethods::class, "use")
            
            getMethod(
                "SRM(net.minecraft.world.level.block.NoteBlock attack)",
                "(L$BLOCK_STATE;L$LEVEL;L$BLOCK_POS;L$PLAYER;)V"
            )!!.instructions = VirtualClassPath.getInstructions(NoteBlockMethods::class, "attack")
            
            getMethod(
                "SRM(net.minecraft.world.level.block.NoteBlock triggerEvent)",
                "(L$BLOCK_STATE;L$LEVEL;L$BLOCK_POS;II)Z"
            )!!.instructions = VirtualClassPath.getInstructions(NoteBlockMethods::class, "triggerEvent")
        }
    }
    
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