package xyz.xenondevs.nova.world.block.migrator

import xyz.xenondevs.nova.util.world.ChunkSearchQuery
import xyz.xenondevs.nova.world.BlockPos

internal data class BlockMigration(val query: ChunkSearchQuery, val migrate: (BlockPos) -> Unit)