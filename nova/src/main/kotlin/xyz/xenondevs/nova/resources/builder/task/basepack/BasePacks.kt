package xyz.xenondevs.nova.resources.builder.task.basepack

import net.minecraft.world.level.block.state.BlockState
import org.bukkit.Material
import xyz.xenondevs.nova.DATA_FOLDER
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.data.PackMcMeta
import xyz.xenondevs.nova.resources.builder.task.MovedFontContent
import xyz.xenondevs.nova.resources.builder.task.PackBuildData
import xyz.xenondevs.nova.resources.builder.task.PackTask
import xyz.xenondevs.nova.resources.builder.task.basepack.merger.FileMerger
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.util.data.readJson
import xyz.xenondevs.nova.util.data.useZip
import xyz.xenondevs.nova.world.block.state.model.BackingStateConfigType
import java.io.File
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

private val BASE_PACKS_DIR: Path = DATA_FOLDER.resolve("resource_pack/base_packs")

private val DEFAULT_WHITELISTED_FILE_TYPES: Set<String> = hashSetOf(
    "json", "png", "mcmeta", "ogg", "txt", "bin", "fsh", "vsh", "glsl", // vanilla
    "properties" // optifine
)

private val WHITELISTED_FILE_TYPES by MAIN_CONFIG.entry<Set<String>>("resource_pack", "generation", "whitelisted_file_types")
    .map { it.mapTo(HashSet(), String::lowercase) + DEFAULT_WHITELISTED_FILE_TYPES }
private val BASE_PACKS by MAIN_CONFIG.entry<List<File>>("resource_pack", "generation", "base_packs")

class BasePacks internal constructor(internal val builder: ResourcePackBuilder) : PackBuildData {
    
    private val mergers = FileMerger.createMergers(this)
    private val packs = (BASE_PACKS + (BASE_PACKS_DIR.toFile().listFiles() ?: emptyArray()))
        .mapTo(HashSet()) { it.absoluteFile } // deduplicate
    
    val packAmount = packs.size
    val occupiedModelData = HashMap<Material, HashSet<Int>>()
    internal val occupiedSolidIds = HashMap<BackingStateConfigType<*>, HashSet<Int>>()
    
    inner class Include : PackTask {
        
        override suspend fun run() {
            for (pack in packs) {
                if (pack.isFile && pack.extension.equals("zip", true)) {
                    pack.useZip { zip -> mergeBasePack(zip) }
                } else if (pack.isDirectory) {
                    mergeBasePack(pack.toPath())
                }
            }
            
            val occupiedBlockStates: Set<BlockState> = occupiedSolidIds.entries.map { (type, ids) ->
                buildSet {
                    for (id in ids) {
                        add(type.of(id, false).vanillaBlockState)
                        if (type.isWaterloggable) {
                            add(type.of(id, true).vanillaBlockState)
                        }
                    }
                }
            }.flatten().toHashSet()
            
            if (occupiedBlockStates.isNotEmpty())
                builder.logger.warn("Base packs occupy ${occupiedBlockStates.size} block states that cannot be used by Nova")
            
            ResourceLookups.OCCUPIED_BLOCK_STATES = occupiedBlockStates
        }
        
        
        private fun mergeBasePack(packDir: Path) {
            try {
                val packMcMetaFile = packDir.resolve("pack.mcmeta")
                if (!packMcMetaFile.exists()) {
                    builder.logger.warn("Skipping base pack $packDir: No pack.mcmeta present")
                    return
                }
                
                val packMcMeta = packMcMetaFile.readJson<PackMcMeta>(true)
                builder.logger.info("Merging base pack \"${packMcMeta.pack.description}\"")
                
                val assetDirs: List<Path> = buildList {
                    add(packDir.resolve("assets"))
                    
                    packMcMeta.overlays?.entries
                        ?.filter { entry -> entry.contains(ResourcePackBuilder.PACK_MAJOR_VERSION, ResourcePackBuilder.PACK_MINOR_VERSION) }
                        ?.forEach { entry -> add(packDir.resolve("${entry.directory}/assets")) }
                }.filter { it.exists() }
                
                for (assetDir in assetDirs) {
                    mergeAssetsDir(assetDir)
                }
                
                requestMovedFonts(packDir)
            } catch (e: Exception) {
                builder.logger.error("Failed to merge base pack in $packDir", e)
            }
        }
        
        private fun mergeAssetsDir(assetsDir: Path) {
            assetsDir.walk()
                .filter(Path::isRegularFile)
                .forEach { sourceFile ->
                    // Validate file extension
                    if (sourceFile.extension.lowercase() !in WHITELISTED_FILE_TYPES) {
                        builder.logger.warn("Skipping file $sourceFile as it is not a resource pack file")
                        return@forEach
                    }
                    
                    // Validate file name
                    if (!ResourcePath.isValidPath(sourceFile.name)) {
                        builder.logger.warn("Skipping file $sourceFile as its name does not match regex [a-z0-9_.-]")
                        return@forEach
                    }
                    
                    // normalize assets dir name to "assets"
                    val relPath = "assets/" + sourceFile.relativeTo(assetsDir).invariantSeparatorsPathString
                    val destFile = builder.resolve(relPath)
                    
                    destFile.parent.createDirectories()
                    val fileMerger = mergers.firstOrNull { it.acceptsFile(relPath) }
                    if (fileMerger != null) {
                        try {
                            fileMerger.merge(sourceFile, destFile)
                        } catch (t: Throwable) {
                            builder.logger.error("An exception occurred trying to merge base pack file \"$sourceFile\" with \"$destFile\"", t)
                        }
                    } else {
                        sourceFile.copyTo(destFile, overwrite = true)
                    }
                }
        }
        
        private fun requestMovedFonts(packDir: Path) {
            val assetsDir = packDir.resolve("assets").takeIf(Path::exists) ?: return
            assetsDir.listDirectoryEntries()
                .mapNotNull { it.resolve("font").takeIf(Path::isDirectory) }
                .forEach { fontDir ->
                    fontDir.walk()
                        .filter { it.isRegularFile() && it.extension.equals("json", true) }
                        .forEach { fontFile ->
                            val fontNameParts = fontFile.relativeTo(assetsDir).invariantSeparatorsPathString
                                .substringBeforeLast('.')
                                .split('/')
                            
                            builder.getBuildData<MovedFontContent>().requestMovedFonts(
                                ResourcePath(ResourceType.Font, fontNameParts[0], fontNameParts.drop(2).joinToString("/")),
                                1..19
                            )
                        }
                }
        }
    }
    
}