package xyz.xenondevs.nova.resources.builder.basepack

import org.bukkit.Material
import xyz.xenondevs.commons.provider.map
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.basepack.merger.FileMerger
import xyz.xenondevs.nova.resources.builder.data.PackMcMeta
import xyz.xenondevs.nova.resources.builder.task.font.MovedFontContent
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

private val DEFAULT_WHITELISTED_FILE_TYPES: Set<String> = hashSetOf(
    "json", "png", "mcmeta", "ogg", "txt", "bin", "fsh", "vsh", "glsl", // vanilla
    "properties" // optifine
)

private val WHITELISTED_FILE_TYPES by MAIN_CONFIG.entry<Set<String>>("resource_pack", "generation", "whitelisted_file_types")
    .map { it.mapTo(HashSet(), String::lowercase) + DEFAULT_WHITELISTED_FILE_TYPES }
private val BASE_PACKS by MAIN_CONFIG.entry<List<File>>("resource_pack", "generation", "base_packs")

class BasePacks internal constructor(private val builder: ResourcePackBuilder) {
    
    private val mergers = FileMerger.createMergers(this)
    private val packs = (BASE_PACKS + (ResourcePackBuilder.BASE_PACKS_DIR.toFile().listFiles() ?: emptyArray()))
        .mapTo(HashSet()) { it.absoluteFile } // deduplicate
    
    val packAmount = packs.size
    val occupiedModelData = HashMap<Material, HashSet<Int>>()
    internal val occupiedSolidIds = HashMap<BackingStateConfigType<*>, HashSet<Int>>()
    
    internal fun include() {
        for (pack in packs) {
            if (pack.isFile && pack.extension.equals("zip", true)) {
                pack.useZip { zip -> mergeBasePack(zip) }
            } else if (pack.isDirectory) {
                mergeBasePack(pack.toPath())
            }
        }
    }
    
    private fun mergeBasePack(packDir: Path) {
        try {
            val packMcMetaFile = packDir.resolve("pack.mcmeta")
            if (!packMcMetaFile.exists()) {
                LOGGER.warn("Skipping base pack $packDir: No pack.mcmeta present")
                return
            }
            
            val packMcMeta = packMcMetaFile.readJson<PackMcMeta>()
            LOGGER.info("Merging base pack \"${packMcMeta.pack.description}\"")
            
            val assetDirs: List<Path> = buildList {
                add(packDir.resolve("assets"))
                
                val packMcMeta = packMcMetaFile.readJson<PackMcMeta>()
                packMcMeta.overlays?.entries
                    ?.filter { entry -> ResourcePackBuilder.PACK_VERSION in entry.formats }
                    ?.forEach { entry -> add(packDir.resolve("${entry.directory}/assets")) }
            }.filter { it.exists() }
            
            for (assetDir in assetDirs) {
                mergeAssetsDir(assetDir)
            }
            
            requestMovedFonts(packDir)
        } catch (e: Exception) {
            LOGGER.error("Failed to merge base pack in $packDir", e)
        }
    }
    
    private fun mergeAssetsDir(assetsDir: Path) {
        assetsDir.walk()
            .filter(Path::isRegularFile)
            .forEach { sourceFile ->
                // Validate file extension
                if (sourceFile.extension.lowercase() !in WHITELISTED_FILE_TYPES) {
                    LOGGER.warn("Skipping file $sourceFile as it is not a resource pack file")
                    return@forEach
                }
                
                // Validate file name
                if (!ResourcePath.isValidPath(sourceFile.name)) {
                    LOGGER.warn("Skipping file $sourceFile as its name does not match regex [a-z0-9_.-]")
                    return@forEach
                }
                
                // normalize assets dir name to "assets"
                val relPath = "assets/" + sourceFile.relativeTo(assetsDir).invariantSeparatorsPathString
                val destFile = ResourcePackBuilder.PACK_DIR.resolve(relPath)
                
                destFile.parent.createDirectories()
                val fileMerger = mergers.firstOrNull { it.acceptsFile(relPath) }
                if (fileMerger != null) {
                    try {
                        fileMerger.merge(sourceFile, destFile)
                    } catch (t: Throwable) {
                        LOGGER.error("An exception occurred trying to merge base pack file \"$sourceFile\" with \"$destFile\"", t)
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
                        
                        builder.getHolder<MovedFontContent>().requestMovedFonts(
                            ResourcePath(ResourceType.Font, fontNameParts[0], fontNameParts.drop(2).joinToString("/")),
                            1..19
                        )
                    }
            }
    }
    
}