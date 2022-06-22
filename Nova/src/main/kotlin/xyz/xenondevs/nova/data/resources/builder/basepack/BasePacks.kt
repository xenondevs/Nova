package xyz.xenondevs.nova.data.resources.builder.basepack

import net.lingala.zip4j.ZipFile
import org.bukkit.Material
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.util.StringUtils
import java.io.File
import java.nio.file.Path

private val WHITELISTED_FILE_TYPES = hashSetOf(
    "json", "png", "mcmeta", "ogg", "txt", "bin", "fsh", "vsh", "glsl", // vanilla
    "properties" // optifine
)

private val BASE_PACKS by configReloadable { DEFAULT_CONFIG.getStringList("resource_pack.base_packs").map(::File) }

internal class BasePacks {
    
    private val mergers = listOf(
        ModelFileMerger(this),
        LangFileMerger(this),
        FontFileMerger(this)
    )
    
    val occupiedModelData = HashMap<Material, HashSet<Int>>()
    
    fun include() {
        BASE_PACKS.map {
            if (it.isFile) {
                val dir = File(ResourcePackBuilder.BASE_PACKS_DIR, it.nameWithoutExtension + StringUtils.randomString(10))
                dir.mkdirs()
                ZipFile(it).extractAll(dir.absolutePath)
                
                return@map dir
            }
            
            return@map it
        }.forEach(::mergeBasePack)
    }
    
    private fun mergeBasePack(packDir: File) {
        LOGGER.info("Adding base pack $packDir")
        packDir.walkTopDown()
            .filter(File::isFile)
            .forEach { file ->
                if (file.extension.lowercase() !in WHITELISTED_FILE_TYPES) {
                    LOGGER.info("Skipping file $file as it is not a resource pack file")
                    return@forEach
                }
                
                val relStr = file.toRelativeString(packDir)
                val relPath = Path.of(relStr)
                val packFile = File(ResourcePackBuilder.PACK_DIR, relStr)
                
                val fileMerger = mergers.firstOrNull { relPath.startsWith(it.path) }
                if (fileMerger != null) {
                    fileMerger.merge(file, packFile)
                } else if (!packFile.exists()) {
                    file.copyTo(packFile)
                } else {
                    LOGGER.warning("Skipping file $file: File type cannot be merged")
                }
            }
    }
    
}