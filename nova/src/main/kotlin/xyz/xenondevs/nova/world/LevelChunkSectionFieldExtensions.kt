package xyz.xenondevs.nova.world

import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.LevelChunkSection
import java.lang.invoke.MethodHandles
import java.lang.invoke.VarHandle

private val LEVEL_CHUNK_SECTION_LEVEL: VarHandle = MethodHandles
    .privateLookupIn(LevelChunkSection::class.java, MethodHandles.lookup())
    .findVarHandle(LevelChunkSection::class.java, $$"nova$level", Level::class.java)

private val LEVEL_CHUNK_SECTION_CHUNK_POS: VarHandle = MethodHandles
    .privateLookupIn(LevelChunkSection::class.java, MethodHandles.lookup())
    .findVarHandle(LevelChunkSection::class.java, $$"nova$chunkPos", ChunkPos::class.java)

private val LEVEL_CHUNK_SECTION_BOTTOM_BLOCK_Y: VarHandle = MethodHandles
    .privateLookupIn(LevelChunkSection::class.java, MethodHandles.lookup())
    .findVarHandle(LevelChunkSection::class.java, $$"nova$bottomBlockY", Int::class.java)

private val LEVEL_CHUNK_SECTION_MIGRATION_ACTIVE: VarHandle = MethodHandles
    .privateLookupIn(LevelChunkSection::class.java, MethodHandles.lookup())
    .findVarHandle(LevelChunkSection::class.java, $$"nova$migrationActive", Boolean::class.java)

internal var LevelChunkSection.level: Level
    get() = LEVEL_CHUNK_SECTION_LEVEL.get(this) as Level
    set(value) = LEVEL_CHUNK_SECTION_LEVEL.set(this, value)

internal var LevelChunkSection.chunkPos: ChunkPos
    get() = LEVEL_CHUNK_SECTION_CHUNK_POS.get(this) as ChunkPos
    set(value) = LEVEL_CHUNK_SECTION_CHUNK_POS.set(this, value)

internal var LevelChunkSection.isMigrationActive: Boolean
    get() = LEVEL_CHUNK_SECTION_MIGRATION_ACTIVE.get(this) as Boolean
    set(value) = LEVEL_CHUNK_SECTION_MIGRATION_ACTIVE.set(this, value)

internal var LevelChunkSection.bottomBlockY: Int
    get() = LEVEL_CHUNK_SECTION_BOTTOM_BLOCK_Y.get(this) as Int
    set(value) = LEVEL_CHUNK_SECTION_BOTTOM_BLOCK_Y.set(this, value)