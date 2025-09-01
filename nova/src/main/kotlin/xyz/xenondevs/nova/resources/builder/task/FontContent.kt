package xyz.xenondevs.nova.resources.builder.task

import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.font.Font
import java.nio.file.Path
import kotlin.collections.iterator
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.walk

/**
 * Contains all fonts of the resource pack.
 */
class FontContent : PackBuildData {
    
    private val _vanillaFonts = HashMap<ResourcePath<ResourceType.Font>, Font>()
    private val _customFonts = HashMap<ResourcePath<ResourceType.Font>, Font>()
    
    /**
     * The fonts of the vanilla assets
     */
    val vanillaFonts: Map<ResourcePath<ResourceType.Font>, Font>
        get() = _vanillaFonts
    
    /**
     * The fonts defined in the resource pack
     */
    val customFonts: Map<ResourcePath<ResourceType.Font>, Font>
        get() = _customFonts
    
    /**
     * A merged view of [vanillaFonts] and [customFonts].
     */
    val mergedFonts: Map<ResourcePath<ResourceType.Font>, Font>
        get() {
            val map = HashMap(_customFonts)
            for ((id, font) in _vanillaFonts) {
                val fontOverride = map[id]
                if (fontOverride != null) {
                    map[id] = Font(id, fontOverride.providers + font.providers)
                } else {
                    map[id] = font
                }
            }
            
            return map
        }
    
    /**
     * Gets the font under [id] or `null` if no such font exists.
     */
    operator fun get(id: ResourcePath<ResourceType.Font>): Font? {
        return _customFonts[id]
    }
    
    /**
     * Gets the font under [id] or creates and registers a new [Font] with the given [id] if no such font exists.
     */
    fun getOrCreate(id: ResourcePath<ResourceType.Font>): Font {
        return _customFonts.getOrPut(id) { Font(id) }
    }
    
    /**
     * Adds [font] to the fonts of this resource pack.
     */
    fun add(font: Font) {
        _customFonts[font.id] = font
    }
    
    /**
     * Adds [font] to the fonts of this resource pack.
     */
    operator fun plusAssign(font: Font) {
        _customFonts[font.id] = font
    }
    
    /**
     * Removes the font with the given [id] from the fonts of this resource pack.
     */
    fun remove(id: ResourcePath<ResourceType.Font>) {
        _customFonts.remove(id)
    }
    
    /**
     * Removes the font with the given [id] from the fonts of this resource pack.
     */
    operator fun minusAssign(id: ResourcePath<ResourceType.Font>) {
        _customFonts.remove(id)
    }
    
    /**
     * Removes [font] from the fonts of this resource pack.
     */
    fun remove(font: Font) {
        _customFonts.remove(font.id)
    }
    
    /**
     * Removes [font] from the fonts of this resource pack.
     */
    operator fun minusAssign(font: Font) {
        _customFonts.remove(font.id)
    }
    
    /**
     * Discovers all fonts, loading [vanillaFonts] and [customFonts].
     */
    inner class DiscoverAllFonts(private val builder: ResourcePackBuilder) : PackTask {
        
        override val runsAfter = setOf(ExtractTask::class)
        
        override suspend fun run() {
            discoverFonts(builder.resolveVanilla("assets/"), _vanillaFonts)
            discoverFonts(builder.resolve("assets/"), _customFonts)
        }
        
        private fun discoverFonts(assetsDir: Path, map: MutableMap<ResourcePath<ResourceType.Font>, Font>) {
            assetsDir.listDirectoryEntries()
                .map { it.resolve("font") }
                .filter { it.exists() }
                .forEach { fontDir ->
                    fontDir.walk()
                        .filter { it.extension.equals("json", true) }
                        .forEach { path ->
                            val font = Font.fromDisk(builder, assetsDir, path)
                            map[font.id] = font
                        }
                }
        }
        
    }
    
    /**
     * Writes all custom fonts in [FontContent] to the resource pack.
     */
    inner class Write(private val builder: ResourcePackBuilder) : PackTask {
        
        override val runsAfter = setOf(DiscoverAllFonts::class)
        
        override suspend fun run() {
            _customFonts.values.forEach { it.write(builder) }
        }
        
    }
    
}