package xyz.xenondevs.nova.world.block.migrator

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.model.ModelLessBlockModelProvider

internal sealed interface BlockMigration {
    val vanillaBlock: Block
}

internal data class SimpleBlockMigration(
    override val vanillaBlock: Block,
    val vanillaBlockState: BlockState
) : BlockMigration

internal data class ComplexBlockMigration(
    override val vanillaBlock: Block,
    val novaBlock: NovaBlock,
    val vanillaToNova: (BlockState) -> NovaBlockState
) : BlockMigration {
    
    fun novaToVanilla(novaBlockState: NovaBlockState): BlockState {
        return (novaBlockState.modelProvider as ModelLessBlockModelProvider).info
    }
    
}