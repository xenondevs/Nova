@file:Suppress("DEPRECATION", "UNUSED_PARAMETER")

package xyz.xenondevs.nova.transformer.patch.block

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
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.nova.data.config.MAIN_CONFIG
import xyz.xenondevs.nova.tileentity.vanilla.VanillaNoteBlockTileEntity
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.transformer.ClassTransformer
import xyz.xenondevs.nova.util.toNovaPos
import xyz.xenondevs.nova.world.block.backingstate.impl.NoteBlockBackingState

internal object NoteBlockPatch : ClassTransformer(NoteBlock::class) {
    
    override fun shouldTransform(): Boolean =
        MAIN_CONFIG.value.node("resource_pack", "generation", "use_solid_blocks").boolean
    
    override fun transform() {
        NoteBlock::neighborChanged.replaceWith(::neighborChanged)
        NoteBlock::use.replaceWith(::use)
        NoteBlock::attack.replaceWith(::attack)
        
        val triggerEvent = VirtualClassPath[NoteBlock::triggerEvent]
        triggerEvent.localVariables.clear()
        triggerEvent.instructions = buildInsnList { ldc(1); ireturn() }
    }
    
    @JvmStatic
    fun neighborChanged(thisRef: NoteBlock, state: BlockState, level: Level, pos: BlockPos, block: Block, neighborPos: BlockPos, flag: Boolean) {
        val vnb = VanillaTileEntityManager.getTileEntityAt(pos.toNovaPos(level.world)) as? VanillaNoteBlockTileEntity ?: return
        
        val shouldBePowered = level.hasNeighborSignal(pos)
        if (shouldBePowered != vnb.powered) {
            vnb.powered = shouldBePowered
            
            if (shouldBePowered)
                NoteBlockBackingState.playNote(vnb)
        }
    }
    
    @JvmStatic
    fun use(thisRef: NoteBlock, state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, result: BlockHitResult): InteractionResult {
        val vnb = VanillaTileEntityManager.getTileEntityAt(pos.toNovaPos(level.world)) as? VanillaNoteBlockTileEntity ?: return InteractionResult.FAIL
        
        NoteBlockBackingState.cycleNote(vnb)
        player.awardStat(Stats.TUNE_NOTEBLOCK)
        
        return InteractionResult.CONSUME
    }
    
    @JvmStatic
    fun attack(thisRef: NoteBlock, state: BlockState, level: Level, pos: BlockPos, player: Player) {
        val vnb = VanillaTileEntityManager.getTileEntityAt(pos.toNovaPos(level.world)) as? VanillaNoteBlockTileEntity ?: return
        
        NoteBlockBackingState.playNote(vnb)
        player.awardStat(Stats.PLAY_NOTEBLOCK)
    }
    
}