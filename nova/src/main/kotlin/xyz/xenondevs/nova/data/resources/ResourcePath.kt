@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.data.resources

import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Path
import kotlin.io.path.exists

data class ResourcePath(val namespace: String, val path: String) {
    
    private val id = "$namespace:$path"
    
    fun getFile(assetsDir: File, extraPath: String? = null, extension: String? = null): File {
        return File(assetsDir, "$namespace/${extraPath ?: ""}/$path" + (extension?.let { ".$it" } ?: ""))
    }
    
    fun getPath(assetsDir: Path, extraPath: String? = null, extension: String? = null): Path {
        return assetsDir.resolve("$namespace/${extraPath ?: ""}/$path" + (extension?.let { ".$it" } ?: ""))
    }
    
    internal fun findInAssetsOrNull(extraPath: String? = null, extension: String? = null): Path? {
        return getPath(ResourcePackBuilder.MCASSETS_ASSETS_DIR, extraPath, extension).takeIf(Path::exists)
            ?: getPath(ResourcePackBuilder.ASSETS_DIR, extraPath, extension).takeIf(Path::exists)
    }
    
    internal fun findInAssets(extraPath: String? = null, extension: String? = null): Path {
        return findInAssetsOrNull(extraPath, extension)
            ?: throw FileNotFoundException("Could not find resource $id (extraPath: $extraPath, extension: $extension)")
    }
    
    override fun equals(other: Any?): Boolean {
        return other is ResourcePath && other.id == id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun toString(): String {
        return id
    }
    
    companion object {
        
        val NAMESPACED_ENTRY = Regex("""^([a-z0-9._-]+):([a-z0-9/._-]+)$""")
        val NON_NAMESPACED_ENTRY = Regex("""^([a-z0-9/._-]+)$""")
        
        fun of(id: String, fallbackNamespace: String = "minecraft"): ResourcePath {
            return if (NON_NAMESPACED_ENTRY.matches(id)) {
                ResourcePath(fallbackNamespace, id)
            } else {
                val match = NAMESPACED_ENTRY.matchEntire(id)
                    ?: throw IllegalArgumentException("Invalid resource id: $id")
                
                ResourcePath(match.groupValues[1], match.groupValues[2])
            }
        }
        
    }
    
}