package xyz.xenondevs.nova.data.resources.builder.basepack

import org.bukkit.Material
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.builder.basepack.merger.FileMerger
import xyz.xenondevs.nova.data.resources.builder.content.armor.ArmorData
import xyz.xenondevs.nova.data.resources.model.blockstate.BlockStateConfigType
import xyz.xenondevs.nova.util.StringUtils
import xyz.xenondevs.nova.util.data.openZip
import java.io.File
import java.nio.file.Path
import java.util.logging.Level
import kotlin.io.path.copyTo
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

private val DEFAULT_WHITELISTED_FILE_TYPES: Set<String> = hashSetOf(
    "json", "png", "mcmeta", "ogg", "txt", "bin", "fsh", "vsh", "glsl", // vanilla
    "properties" // optifine
)

private val WHITELISTED_FILE_TYPES: Set<String> by configReloadable {
    DEFAULT_CONFIG.getStringList("resource_pack.generation.whitelisted_file_types").mapTo(HashSet()) { it.lowercase() } + DEFAULT_WHITELISTED_FILE_TYPES
}

private val BASE_PACKS by configReloadable { DEFAULT_CONFIG.getStringList("resource_pack.generation.base_packs").map(::File) }

internal class BasePacks {
    
    private val mergers = FileMerger.createMergers(this)
    private val packs = BASE_PACKS + (ResourcePackBuilder.BASE_PACKS_DIR.toFile().listFiles() ?: emptyArray())
    
    val packAmount = packs.size
    val occupiedModelData = HashMap<Material, HashSet<Int>>()
    val occupiedSolidIds = HashMap<BlockStateConfigType<*>, HashSet<Int>>()
    val customArmor = HashMap<Int, ArmorData>()
    
    fun include() {
        packs.map {
            if (it.isFile && it.extension.equals("zip", true)) {
                val dir = ResourcePackBuilder.TEMP_BASE_PACKS_DIR.resolve("${it.nameWithoutExtension}-${StringUtils.randomString(5)}")
                dir.createDirectories()
                it.openZip().copyToRecursively(dir, followLinks = false, overwrite = true)
                
                return@map dir
            }
            
            return@map it.toPath()
        }.forEach(::mergeBasePack)
    }
    
    private fun mergeBasePack(packDir: Path) {
        LOGGER.info("Adding base pack $packDir")
        packDir.walk()
            .filter(Path::isRegularFile)
            .forEach { file ->
                // Validate file extension
                if (file.extension.lowercase() !in WHITELISTED_FILE_TYPES) {
                    LOGGER.warning("Skipping file $file as it is not a resource pack file")
                    return@forEach
                }
                
                // Validate file name
                if (!ResourcePath.NON_NAMESPACED_ENTRY.matches(file.name)) {
                    LOGGER.warning("Skipping file $file as its name does not match regex ${ResourcePath.NON_NAMESPACED_ENTRY}")
                    return@forEach
                }
                
                val relPath = file.relativeTo(packDir)
                val packFile = ResourcePackBuilder.PACK_DIR.resolve(relPath)
                
                packFile.parent.createDirectories()
                val fileMerger = mergers.firstOrNull { it.acceptsFile(relPath) }
                if (fileMerger != null) {
                    try {
                        fileMerger.merge(file, packFile, relPath)
                    } catch (t: Throwable) {
                        LOGGER.log(Level.SEVERE, "An exception occurred trying to merge base pack file \"$file\" with \"$packFile\"", t)
                    }
                } else if (!packFile.exists()) {
                    file.copyTo(packFile)
                } else {
                    LOGGER.warning("Skipping file $file: File type cannot be merged")
                }
            }
    }
    
}