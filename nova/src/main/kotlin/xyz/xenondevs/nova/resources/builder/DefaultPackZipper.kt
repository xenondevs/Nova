package xyz.xenondevs.nova.resources.builder

import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.inputStream
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

private val COMPRESSION_LEVEL by MAIN_CONFIG.entry<Int>("resource_pack", "generation", "compression_level")

/**
 * The default implementation of [PackZipper], respecting resource filters.
 */
internal class DefaultPackZipper(private val builder: ResourcePackBuilder) : PackZipper {
    
    override fun createZip(): ByteArray {
        val root = builder.resolve(".")
        val filters = builder.getResourceFilters(ResourceFilter.Stage.RESOURCE_PACK)
        
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.setLevel(COMPRESSION_LEVEL)
            root.walk()
                .filter { path -> path.isRegularFile() }
                .filter { path -> filters.all { filter -> filter.allows(path.relativeTo(builder.resolve("assets/")).invariantSeparatorsPathString) } }
                .forEach { path ->
                    zip.putNextEntry(ZipEntry(path.relativeTo(root).invariantSeparatorsPathString))
                    path.inputStream().use { it.copyTo(zip) }
                }
        }
        
        return out.toByteArray()
    }
    
}