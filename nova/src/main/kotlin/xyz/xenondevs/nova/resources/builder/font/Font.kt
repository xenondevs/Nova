package xyz.xenondevs.nova.resources.builder.font

import com.google.gson.JsonObject
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import xyz.xenondevs.commons.gson.getArray
import xyz.xenondevs.commons.gson.parseJson
import xyz.xenondevs.commons.gson.toJsonArray
import xyz.xenondevs.commons.gson.writeToFile
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.font.provider.FontProvider
import xyz.xenondevs.nova.resources.builder.font.provider.ReferenceProvider
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.relativeTo

class Font(
    val id: ResourcePath<ResourceType.Font>,
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
     * Gets all occupied code points by resolving reference providers using the given [fonts].
     */
    fun getCodePoints(fonts: Iterable<Font>): IntSet {
        val codePoints = IntOpenHashSet()
        for (provider in providers) {
            if (provider is ReferenceProvider) {
                val font = fonts.firstOrNull { it.id == provider.id }
                    ?: throw IllegalArgumentException("Referenced font ${provider.id} not found")
                codePoints.addAll(font.getCodePoints(fonts))
            } else {
                codePoints.addAll(provider.codePoints)
            }
        }
        
        return codePoints
    }
    
    /**
     * Writes this [Font] to its corresponding file in the given [assetsDir].
     *
     * Depending on the providers, additional files (such as bitmaps or unihex zips) might be written to the [assetsDir].
     */
    fun write(builder: ResourcePackBuilder) {
        val file = builder.resolve(id)
        
        val obj = JsonObject()
        val providers = providers.map { it.write(builder); it.toJson() }
        obj.add("providers", providers.toJsonArray())
        
        file.parent.createDirectories()
        obj.writeToFile(file)
    }
    
    override fun toString() = id.toString()
    
    companion object {
        
        val DEFAULT = ResourcePath(ResourceType.Font, "minecraft", "default")
        val UNIFORM = ResourcePath(ResourceType.Font, "minecraft", "uniform")
        val PRIVATE_USE_AREA = 0xE000..0xF8FF
        
        private val FONT_NAME_REGEX = Regex("""^([a-z0-9._-]+)/font/([a-z0-9/._-]+)$""")
        
        fun fromDisk(builder: ResourcePackBuilder, assetsDir: Path, fontFile: Path): Font {
            val id = readIdFromPath(assetsDir, fontFile)
            val providers = fontFile.parseJson().asJsonObject.getArray("providers")
                .mapTo(ArrayList()) { FontProvider.fromDisk(builder, it.asJsonObject) }
            
            return Font(id, providers)
        }
        
        private fun readIdFromPath(assetsDir: Path, file: Path): ResourcePath<ResourceType.Font> {
            val relPath = file.relativeTo(assetsDir)
                .invariantSeparatorsPathString
                .substringBeforeLast('.') // example: minecraft/font/default
            
            val result = FONT_NAME_REGEX.matchEntire(relPath)
                ?: throw IllegalArgumentException("File $file is not a font file")
            
            return ResourcePath(ResourceType.Font, result.groupValues[1], result.groupValues[2])
        }
        
        /**
         * Finds the first range of undefined characters inside the give [between] range that is [size] characters long.
         */
        fun findFirstUnoccupiedRange(codePoints: IntSet, between: IntRange, size: Int): IntRange {
            var start = between.first
            var i = start
            
            while (i <= between.last) {
                if (i in codePoints) {
                    start = i + 1
                } else if (i - start == size) {
                    return start..i
                }
                
                i++
            }
            
            throw IllegalStateException("No unoccupied range of size $size found in $between")
        }
        
        /**
         * Finds the first undefined character inside the give [between] range.
         */
        fun findFirstUnoccupied(codePoints: IntSet, between: IntRange): Int {
            for (i in between) {
                if (i !in codePoints)
                    return i
            }
            
            throw IllegalStateException("No unoccupied character found in $between")
        }
        
    }
    
}