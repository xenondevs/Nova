package xyz.xenondevs.nova.world.format.legacy.world.v2

import net.kyori.adventure.key.Key
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.commons.collections.associateWithNotNull
import xyz.xenondevs.commons.collections.takeUnlessEmpty
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.util.levelChunk
import xyz.xenondevs.nova.util.nmsBlockEntity
import xyz.xenondevs.nova.util.nmsPos
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.toNamespacedKey
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.holder.VanillaFluidHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.VanillaItemHolder
import xyz.xenondevs.nova.world.block.tileentity.vanilla.VanillaCauldronBlockEntity
import xyz.xenondevs.nova.world.format.WorldDataManager
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes

private const val CONVERTED_WORLDS_KEY = "legacy_region_converted_worlds"
private const val CHUNKS_PER_UNLOAD = 64
private val REGION_FILE_NAME = Regex("""r\.(-?\d+)\.(-?\d+)\.nvr""")

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    runBefore = [WorldDataManager::class]
)
internal object LegacyRegionFileConverter {
    
    @InitFun
    private fun convert() {
        val convertedWorlds = PermanentStorage.retrieve<Set<Key>>(CONVERTED_WORLDS_KEY).orEmpty().toMutableSet()
        for (world in Bukkit.getWorlds()) {
            if (world.key in convertedWorlds)
                continue
            
            val regionDirectory = world.worldFolder.toPath().resolve("nova_region")
            if (!regionDirectory.exists())
                continue
            
            val regionFiles = regionDirectory.listDirectoryEntries("*.nvr")
                .filter(Path::isRegularFile)
                .associateWithNotNull { path -> parseRegionCoordinates(path) }
                .takeUnlessEmpty()
                ?: continue
            
            convert(world, regionFiles)
            world.save()
            convertedWorlds += world.key
            PermanentStorage.store(CONVERTED_WORLDS_KEY, convertedWorlds)
        }
    }
    
    private fun convert(world: World, regionFiles: Map<Path, Pair<Int, Int>>) {
        LOGGER.info("Converting ${regionFiles.size} legacy Nova region files in ${world.name}")
        var blockCount = 0
        var teCount = 0
        var vteCount = 0
        var chunksSinceUnload = 0
        
        try {
            for ([file, coords] in regionFiles) {
                val [rx, rz] = coords
                val region = LegacyRegionFile.read(ByteReader.fromByteArray(file.readBytes()), world, rx, rz)
                
                for (chunk in region.chunks) {
                    if (chunk.isEmpty)
                        continue
                    
                    val levelChunk = world.getChunkAt(chunk.pos.x, chunk.pos.z).levelChunk
                    
                    // Migrate Nova blocks
                    chunk.forEachBlock { block, state ->
                        val data = chunk.tileEntityData[block]
                        if (data != null && state.block !is NovaTileEntityBlock)
                            LOGGER.warn("Block at ${block.x}, ${block.y}, ${block.z} has data but is not a tile entity anymore")
                        
                        setBlockState(levelChunk, block, state, data)
                        
                        blockCount++
                    }
                    teCount += chunk.tileEntityData.size
                    
                    // Migrate VTEs
                    for ([block, data] in chunk.vanillaTileEntityData) {
                        migrateVanillaTileEntity(block, data)
                        vteCount++
                    }
                    
                    if (++chunksSinceUnload == CHUNKS_PER_UNLOAD) {
                        saveAndUnloadChunks(world)
                        chunksSinceUnload = 0
                    }
                }
                if (chunksSinceUnload > 0) {
                    saveAndUnloadChunks(world)
                    chunksSinceUnload = 0
                }
                
                LOGGER.info("Converted legacy Nova region file ${file.name} in ${world.key.asString()}")
            }
            
            LOGGER.info("Conversion for ${world.key.asString()} completed! $blockCount blocks, $teCount TEs, $vteCount VTEs")
        } catch (t: Throwable) {
            throw IllegalStateException("Failed to convert legacy Nova region files in ${world.name}.", t)
        }
    }
    
    private fun saveAndUnloadChunks(world: World) {
        val level = world.serverLevel
        val previousNoSave = level.noSave
        level.noSave = false
        try {
            // process chunk unload now to prevent OOM
            val manager = level.`moonrise$getChunkTaskScheduler`().chunkHolderManager
            manager.processTicketUpdates()
            var previousRetained = Int.MAX_VALUE
            while (true) {
                val retained = manager.chunkHolders.count { it.currentChunk != null }
                if (retained >= previousRetained)
                    break
                previousRetained = retained
                manager.processUnloads()
            }
            level.chunkSource.save(true)
        } finally {
            level.noSave = previousNoSave
        }
    }
    
    private fun setBlockState(chunk: LevelChunk, block: Block, state: BlockState, data: Compound?) {
        val pos = block.nmsPos
        val section = chunk.getSection(chunk.getSectionIndex(pos.y))
        val previousState = section.setBlockState(pos.x and 0xF, pos.y and 0xF, pos.z and 0xF, state)
        if (previousState !== state) {
            // remove previous block entity (e.g. if previously backed by a block entity)
            chunk.removeBlockEntity(pos)
            
            // create new block entity if tile entity block
            val teProxy = (state.block as? NovaTileEntityBlock?)?.blockEntityType?.create(pos, state)
            if (teProxy != null) {
                if (data != null)
                    teProxy.data.putAll(data)
                chunk.addAndRegisterBlockEntity(teProxy)
            }
            
            // load model provider
            (state.block as? NovaBlock)?.modelProviders?.get()?.get(state)?.load(block)
            
            // mark as dirty
            chunk.markUnsaved()
        }
    }
    
    private fun migrateVanillaTileEntity(block: Block, data: Compound) {
        val blockEntity = getOrCreateBlockEntity(block)
        if (blockEntity == null) {
            LOGGER.warn("Block at ${block.x}, ${block.y}, ${block.z} has data but is not a vanilla tile entity anymore")
            return
        }
        
        val pdc = blockEntity.persistentDataContainer
        data.get<Compound>("itemHolder")?.also { from ->
            moveToPdc(from, "connectionConfig", pdc, VanillaItemHolder.CONNECTION_CONFIG)
            moveToPdc(from, "insertFilters", pdc, VanillaItemHolder.INSERT_FILTERS)
            moveToPdc(from, "extractFilters", pdc, VanillaItemHolder.EXTRACT_FILTERS)
            moveToPdc(from, "channels", pdc, VanillaItemHolder.CHANNELS)
            moveToPdc(from, "insertPriorities", pdc, VanillaItemHolder.INSERT_PRIORITIES)
            moveToPdc(from, "extractPriorities", pdc, VanillaItemHolder.EXTRACT_PRIORITIES)
            if (from.isNotEmpty())
                LOGGER.info("Unmigrated itemHolder data for VTE at ${block.x}, ${block.y}, ${block.z}: ${from.keys}")
        }
        data["itemHolder"] = null
        
        data.get<Compound>("fluidHolder")?.also { from ->
            moveToPdc(from, "connectionConfig", pdc, VanillaFluidHolder.CONNECTION_CONFIG)
            moveToPdc(from, "channels", pdc, VanillaFluidHolder.CHANNELS)
            moveToPdc(from, "insertPriorities", pdc, VanillaFluidHolder.INSERT_PRIORITIES)
            moveToPdc(from, "extractPriorities", pdc, VanillaFluidHolder.EXTRACT_PRIORITIES)
            if (from.isNotEmpty())
                LOGGER.info("Unmigrated fluidHolder data for VTE at ${block.x}, ${block.y}, ${block.z}: ${from.keys}")
        }
        data["fluidHolder"] = null
        
        // not needed
        data["type"] = null
        
        if (data.isNotEmpty())
            LOGGER.info("Unmigrated data for VTE at ${block.x}, ${block.y}, ${block.z}: ${data.keys}")
        
        blockEntity.setChanged()
    }
    
    private fun getOrCreateBlockEntity(block: Block): BlockEntity? {
        val blockEntity = block.nmsBlockEntity
        if (blockEntity != null)
            return blockEntity
        
        val level = block.world.serverLevel
        val state = level.getBlockState(block.nmsPos)
        if (state.block !== Blocks.CAULDRON && state.block !== Blocks.WATER_CAULDRON && state.block !== Blocks.LAVA_CAULDRON)
            return null
        
        return VanillaCauldronBlockEntity(block.nmsPos, state).also(level::setBlockEntity)
    }
    
    private fun moveToPdc(from: Compound, fromKey: String, to: PersistentDataContainer, toKey: Key) {
        val data = from.getSerialized(fromKey)
            ?: return
        to.set(toKey.toNamespacedKey(), PersistentDataType.BYTE_ARRAY, data)
        from[fromKey] = null
    }
    
    private fun parseRegionCoordinates(path: Path): Pair<Int, Int>? {
        val match = REGION_FILE_NAME.matchEntire(path.fileName.toString()) ?: return null
        return match.groupValues[1].toInt() to match.groupValues[2].toInt()
    }
    
}
