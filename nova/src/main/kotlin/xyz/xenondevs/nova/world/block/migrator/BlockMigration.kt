package xyz.xenondevs.nova.world.block.migrator

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.state.NovaBlockState

internal data class BlockMigration(
    val vanillaBlock: Block,
    val novaBlock: NovaBlock?,
    val vanillaBlockState: BlockState,
    val vanillaToNova: ((BlockState) -> NovaBlockState)? = null,
    val novaToVanilla: ((NovaBlockState) -> BlockState)? = null
)