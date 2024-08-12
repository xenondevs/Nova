package xyz.xenondevs.nova.patch.impl.block

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.HugeMushroomBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.NoteBlock
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionUtils

private val NOTE_BLOCK_UPDATE_SHAPE = ReflectionUtils.getMethod(
    NoteBlock::class,
    "updateShape",
    BlockState::class, Direction::class, BlockState::class, LevelAccessor::class, BlockPos::class, BlockPos::class
)

private val NOTE_BLOCK_NEIGHBOR_CHANGED = ReflectionUtils.getMethod(
    NoteBlock::class,
    "neighborChanged",
    BlockState::class, Level::class, BlockPos::class, Block::class, BlockPos::class, Boolean::class
)

private val NOTE_BLOCK_USE_ITEM_ON = ReflectionUtils.getMethod(
    NoteBlock::class,
    "useItemOn",
    ItemStack::class, BlockState::class, Level::class, BlockPos::class, Player::class, InteractionHand::class, BlockHitResult::class
)

private val NOTE_BLOCK_USE_WITHOUT_ITEM = ReflectionUtils.getMethod(
    NoteBlock::class,
    "useWithoutItem",
    BlockState::class, Level::class, BlockPos::class, Player::class, BlockHitResult::class
)

private val NOTE_BLOCK_ATTACK = ReflectionUtils.getMethod(
    NoteBlock::class,
    "attack",
    BlockState::class, Level::class, BlockPos::class, Player::class
)

private val NOTE_BLOCK_TRIGGER_EVENT = ReflectionUtils.getMethod(
    NoteBlock::class,
    "triggerEvent",
    BlockState::class, Level::class, BlockPos::class, Int::class, Int::class
)

private val HUGE_MUSHROOM_BLOCK_UPDATE_SHAPE = ReflectionUtils.getMethod(
    HugeMushroomBlock::class,
    "updateShape",
    BlockState::class, Direction::class, BlockState::class, LevelAccessor::class, BlockPos::class, BlockPos::class
)

private val HUGE_MUSHROOM_BLOCK_ROTATE = ReflectionUtils.getMethod(
    HugeMushroomBlock::class,
    "rotate",
    BlockState::class, Rotation::class
)

private val HUGE_MUSHROOM_BLOCK_MIRROR = ReflectionUtils.getMethod(
    HugeMushroomBlock::class,
    "mirror",
    BlockState::class, Mirror::class
)

internal object DisableBackingStateLogicPatch : MultiTransformer(NoteBlock::class, HugeMushroomBlock::class) {
    
    override fun transform() {
        val returnDefaultBlockState = buildInsnList {
            addLabel()
            aLoad(0)
            invokeVirtual(Block::defaultBlockState)
            areturn()
        }
        
        val returnFirst = buildInsnList {
            addLabel()
            aLoad(1)
            areturn()
        }
        
        val emptyInsn = buildInsnList {
            addLabel()
            _return()
        }
        
        val failItemInteraction = buildInsnList {
            addLabel()
            getStatic(ItemInteractionResult.FAIL)
            areturn()
        }
        
        val passInteraction = buildInsnList {
            addLabel()
            getStatic(InteractionResult.PASS)
            areturn()
        }
        
        val returnFalse = buildInsnList {
            addLabel()
            ldc(0)
            ireturn()
        }
        
        VirtualClassPath[NOTE_BLOCK_UPDATE_SHAPE].replaceInstructions(returnFirst)
        VirtualClassPath[NOTE_BLOCK_NEIGHBOR_CHANGED].replaceInstructions(emptyInsn)
        VirtualClassPath[NOTE_BLOCK_USE_ITEM_ON].replaceInstructions(failItemInteraction)
        VirtualClassPath[NOTE_BLOCK_USE_WITHOUT_ITEM].replaceInstructions(passInteraction)
        VirtualClassPath[NOTE_BLOCK_ATTACK].replaceInstructions(emptyInsn)
        VirtualClassPath[NOTE_BLOCK_TRIGGER_EVENT].replaceInstructions(returnFalse)
        
        VirtualClassPath[HUGE_MUSHROOM_BLOCK_UPDATE_SHAPE].replaceInstructions(returnFirst)
        VirtualClassPath[HUGE_MUSHROOM_BLOCK_ROTATE].replaceInstructions(returnDefaultBlockState)
        VirtualClassPath[HUGE_MUSHROOM_BLOCK_MIRROR].replaceInstructions(returnDefaultBlockState)
    }
    
}