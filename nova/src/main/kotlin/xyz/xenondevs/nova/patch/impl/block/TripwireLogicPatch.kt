package xyz.xenondevs.nova.patch.impl.block

import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.TripWireHookBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FlowingFluid
import net.minecraft.world.level.material.Fluid
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.replaceEvery
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.util.toNovaPos
import xyz.xenondevs.nova.world.block.DefaultBlocks
import xyz.xenondevs.nova.world.block.behavior.TripwireBehavior
import xyz.xenondevs.nova.world.format.WorldDataManager

private val FLOWING_FLUID_CAN_HOLD_FLUID = ReflectionUtils.getMethod(
    FlowingFluid::class, 
    "canHoldFluid",
    BlockGetter::class, BlockPos::class, BlockState::class, Fluid::class
)

internal object TripwireLogicPatch : MultiTransformer(TripWireHookBlock::class, FlowingFluid::class) {
    
    override fun transform() {
        VirtualClassPath[TripWireHookBlock::calculateState].replaceEvery(
            0, 0,
            { invokeStatic(::getBlockState) }
        ) {it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(Level::getBlockState)}
        
        VirtualClassPath[FLOWING_FLUID_CAN_HOLD_FLUID].instructions.insert(buildInsnList { 
            addLabel()
            aLoad(1) // level
            aLoad(2) // pos
            aLoad(3) // state
            invokeStatic(::cannotHoldFluid)
            
            // if (cannotHoldFluid(level, pos, state)) return false
            val continueLabel = LabelNode()
            ifeq(continueLabel)
            ldc(0)
            ireturn()
            add(continueLabel)
        })
    }
    
    @JvmStatic
    fun getBlockState(level: Level, pos: BlockPos): BlockState {
        val novaPos = pos.toNovaPos(level.world)
        val novaState = WorldDataManager.getBlockState(novaPos)
        if (novaState?.block == DefaultBlocks.TRIPWIRE) {
            return TripwireBehavior.vanillaBlockStateOf(novaState)
        }
        
        return level.getBlockState(pos)
    }
    
    @JvmStatic
    fun cannotHoldFluid(level: BlockGetter, pos: BlockPos, state: BlockState): Boolean {
        if (level !is Level)
            return false
        
        if (state.block == Blocks.TRIPWIRE) {
            val novaState = WorldDataManager.getBlockState(pos.toNovaPos(level.world))
            if (novaState != null && novaState.block != DefaultBlocks.TRIPWIRE)
                return true
        }
        
        return false
    }
    
}