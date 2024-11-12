package xyz.xenondevs.nova.patch.impl.block

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.TripWireHookBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FlowingFluid
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.replaceEvery
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.util.toNovaPos
import xyz.xenondevs.nova.world.block.DefaultBlocks
import xyz.xenondevs.nova.world.block.behavior.TripwireBehavior
import xyz.xenondevs.nova.world.format.WorldDataManager

internal object TripwireLogicPatch : MultiTransformer(TripWireHookBlock::class, FlowingFluid::class) {
    
    override fun transform() {
        VirtualClassPath[TripWireHookBlock::calculateState].replaceEvery(
            0, 0,
            { invokeStatic(::getBlockState) }
        ) {it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(Level::getBlockState)}
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
    
}