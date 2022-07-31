package xyz.xenondevs.nova.data.world

import org.bukkit.World
import xyz.xenondevs.nova.world.ChunkPos

internal interface Task

internal open class ChunkTask(val pos: ChunkPos) : Task

internal class ChunkLoadTask(pos: ChunkPos) : ChunkTask(pos)

internal class ChunkUnloadTask(pos: ChunkPos) : ChunkTask(pos)

internal open class WorldTask(val world: World) : Task

internal class SaveWorldTask(world: World) : WorldTask(world)

internal class WorldUnloadTask(world: World) : WorldTask(world)
