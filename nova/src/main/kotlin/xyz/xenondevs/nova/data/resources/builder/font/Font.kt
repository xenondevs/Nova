package xyz.xenondevs.nova.data.resources.builder.font

import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.getArray
import xyz.xenondevs.commons.gson.parseJson
import xyz.xenondevs.commons.gson.toJsonArray
import xyz.xenondevs.commons.gson.writeToFile
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.font.provider.FontProvider
import xyz.xenondevs.nova.data.resources.builder.font.provider.ReferenceProvider
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.relativeTo

class Font(
    val id: ResourcePath,
    providers: List<FontProvider> = emptyList()
) {
    
    private val _providers = providers.toMutableList()
    val providers: List<FontProvider> get() = _providers
    
    /**
     * Adds the given [provider] to the start of the list.
     */
    fun addFirst(provider: FontProvider) {
        _providers.add(0, provider)
    }
    
    /**
     * Adds the given [provider] to the end of the list.
     */
    fun add(provider: FontProvider) {
        _providers += provider
    }
    
    /**
     * Adds all [providers] of the given font to the end of the list.
     */
    fun addAll(other: Font) {
        _providers += other.providers
    }
    
    /**
     * Adds the given [provider] to the end of the list.
     */
    operator fun plusAssign(provider: FontProvider) {
        _providers += provider
    }
    
    /**
     * Adds the given [providers] to the end of the list.
     */
    operator fun plusAssign(providers: Iterable<FontProvider>) {
        _providers += providers
    }
    
    /**
     * Adds all [providers] of the given font to the end of the list.
     */
    operator fun plusAssign(font: Font) {
        _providers += font.providers
    }
    
    /**
     * Removes the given [provider] from the list.
     */
    fun remove(provider: FontProvider) {
        _providers -= provider
    }
    
    /**
     * Maps every reference provider of this font to the corresponding font from [fonts].
     *
     * @throws IllegalArgumentException If a referenced font is not present in the given [fonts].
     */
    fun mapReferences(fonts: Iterable<Font>): Set<Font> {
        return providers.asSequence()
            .filterIsInstance<ReferenceProvider>()
            .mapTo(HashSet()) { ref -> fonts.firstOrNull { ref.id == it.id } ?: throw IllegalArgumentException("Referenced font ${ref.id} not found") }
    }
    
    /**
     * Writes this [Font] to its corresponding file in the given [assetsDir].
     * 
     * Depending on the providers, additional files (such as bitmaps or unihex zips) might be written to the [assetsDir].
     */
    fun write(assetsDir: Path) {
        val file = id.getPath(assetsDir, "font", "json")
        
        val obj = JsonObject()
        val providers = providers.map { it.write(assetsDir); it.toJson() }
        obj.add("providers", providers.toJsonArray())
        
        file.parent.createDirectories()
        obj.writeToFile(file)
    }
    
    override fun toString() = id.toString()
    
    companion object {
        
        private val FONT_NAME_REGEX = Regex("""^([a-z0-9._-]+)/font/([a-z0-9/._-]+)$""")
        
        fun fromDisk(assetsDir: Path, fontFile: Path): Font {
            val id = readIdFromPath(assetsDir, fontFile)
            val providers = fontFile.parseJson().asJsonObject.getArray("providers")
                .mapTo(ArrayList()) { FontProvider.fromDisk(assetsDir, it.asJsonObject) }
            
            return Font(id, providers)
        }
        
        private fun readIdFromPath(assetsDir: Path, file: Path): ResourcePath {
            val relPath = file.relativeTo(assetsDir)
                .invariantSeparatorsPathString
                .substringBeforeLast('.') // example: minecraft/font/default
            
            val result = FONT_NAME_REGEX.matchEntire(relPath)
                ?: throw IllegalArgumentException("File $file is not a font file")
            
            return ResourcePath(result.groupValues[1], result.groupValues[2])
        }
        
    }
    
}