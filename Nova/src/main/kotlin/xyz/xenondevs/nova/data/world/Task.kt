package xyz.xenondevs.nova.data.world

import org.bukkit.World
import xyz.xenondevs.nova.world.ChunkPos

open class Task

open class ChunkTask(val pos: ChunkPos) : Task()

class ChunkLoadTask(pos: ChunkPos) : ChunkTask(pos)

class SaveWorldTask(val world: World) : Task()
