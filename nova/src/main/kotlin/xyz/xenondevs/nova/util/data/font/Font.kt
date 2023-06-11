package xyz.xenondevs.nova.util.data.font

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.getArray
import xyz.xenondevs.commons.gson.getString
import xyz.xenondevs.commons.gson.parseJson
import xyz.xenondevs.nova.data.resources.ResourcePath
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.relativeTo

private val FONT_NAME_REGEX = Regex("""^([a-z0-9._-]+)/font/([a-z0-9/._-]+)$""")

internal class Font(val id: ResourcePath, val obj: JsonObject) {
    
    val providers: JsonArray = obj.getArray("providers")
    
    lateinit var referenceProviders: Set<Font> private set
    
    /**
     * Merges the providers of the [other] font into this font.
     */
    fun merge(other: Font) {
        require(!::referenceProviders.isInitialized) { "Cannot merge into font with loaded references" }
        providers.addAll(other.providers)
    }
    
    fun loadReferences(files: Iterable<Font>) {
        referenceProviders = providers.asSequence()
            .filterIsInstance<JsonObject>()
            .filter { it.getString("type") == "reference" }
            .mapTo(HashSet()) { provider ->
                val id = ResourcePath.of(provider.getString("id"))
                files.first { it.id == id }
            }
    }
    
    override fun toString() = id.toString()
    
    companion object {
        
        fun fromFile(base: Path, file: Path) =
            Font(readIdFromPath(base, file), file.parseJson() as JsonObject)
        
        private fun readIdFromPath(base: Path, file: Path): ResourcePath {
            val relPath = file.relativeTo(base)
                .invariantSeparatorsPathString
                .substringBeforeLast('.') // example: minecraft/font/default
            
            val result = FONT_NAME_REGEX.matchEntire(relPath)
                ?: throw IllegalArgumentException("File $file is not a font file")
            
            return ResourcePath(result.groupValues[1], result.groupValues[2])
        }
        
    }
    
}