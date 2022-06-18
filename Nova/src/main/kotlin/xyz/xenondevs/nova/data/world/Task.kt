package xyz.xenondevs.nova.data.world

import org.bukkit.World
import xyz.xenondevs.nova.world.ChunkPos

internal interface Task

internal open class ChunkTask(val pos: ChunkPos) : Task

internal class ChunkLoadTask(pos: ChunkPos) : ChunkTask(pos)

internal class ChunkUnloadTask(pos: ChunkPos) : ChunkTask(pos)

internal class SaveWorldTask(val world: World) : Task
