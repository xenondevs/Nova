package xyz.xenondevs.nova.patch.impl.block

import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FlowingFluid
import net.minecraft.world.level.material.Fluid
import org.objectweb.asm.tree.LabelNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.util.toNovaPos
import xyz.xenondevs.nova.world.block.DefaultBlocks
import xyz.xenondevs.nova.world.block.behavior.Waterloggable
import xyz.xenondevs.nova.world.format.WorldDataManager

private val FLOWING_FLUID_CAN_HOLD_FLUID = ReflectionUtils.getMethod(
    FlowingFluid::class,
    "canHoldFluid",
    BlockGetter::class, BlockPos::class, BlockState::class, Fluid::class
)

internal object FluidFlowPatch : MultiTransformer(FlowingFluid::class) {
    
    override fun transform() {
        VirtualClassPath[FLOWING_FLUID_CAN_HOLD_FLUID].instructions.insert(buildInsnList {
            addLabel()
            aLoad(0) // level
            aLoad(1) // pos
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
    fun cannotHoldFluid(level: BlockGetter, pos: BlockPos): Boolean {
        if (level !is Level)
            return false
        
        val novaBlock = WorldDataManager.getBlockState(pos.toNovaPos(level.world))?.block
        return novaBlock != null && !novaBlock.hasBehavior<Waterloggable>() && novaBlock != DefaultBlocks.TRIPWIRE
    }
    
}