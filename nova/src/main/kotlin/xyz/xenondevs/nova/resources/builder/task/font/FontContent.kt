package xyz.xenondevs.nova.resources.builder.task.font

import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder.Companion.ASSETS_DIR
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder.Companion.MCASSETS_ASSETS_DIR
import xyz.xenondevs.nova.resources.builder.font.Font
import xyz.xenondevs.nova.resources.builder.task.PackTask
import xyz.xenondevs.nova.resources.builder.task.PackTaskHolder
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.walk

class FontContent internal constructor(private val builder: ResourcePackBuilder) : PackTaskHolder {
    
    private val _vanillaFonts = HashMap<ResourcePath, Font>()
    val vanillaFonts: Map<ResourcePath, Font>
        get() = _vanillaFonts
    
    private val _customFonts = HashMap<ResourcePath, Font>()
    val customFonts: Map<ResourcePath, Font>
        get() = _customFonts
    
    val mergedFonts: Map<ResourcePath, Font>
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
    
    operator fun get(id: ResourcePath): Font? {
        return _customFonts[id]
    }
    
    fun getOrCreate(id: ResourcePath): Font {
        return _customFonts.getOrPut(id) { Font(id) }
    }
    
    fun add(font: Font) {
        _customFonts[font.id] = font
    }
    
    operator fun plusAssign(font: Font) {
        _customFonts[font.id] = font
    }
    
    fun remove(id: ResourcePath) {
        _customFonts.remove(id)
    }
    
    operator fun minusAssign(id: ResourcePath) {
        _customFonts.remove(id)
    }
    
    fun remove(font: Font) {
        _customFonts.remove(font.id)
    }
    
    operator fun minusAssign(font: Font) {
        _customFonts.remove(font.id)
    }
    
    @PackTask(runAfter = ["ExtractTask#extractAll"])
    private fun discoverAllFonts() {
        discoverFonts(MCASSETS_ASSETS_DIR, _vanillaFonts)
        discoverFonts(ASSETS_DIR, _customFonts)
    }
    
    private fun discoverFonts(assetsDir: Path, map: MutableMap<ResourcePath, Font>) {
        assetsDir.listDirectoryEntries()
            .map { it.resolve("font") }
            .filter { it.exists() }
            .forEach { fontDir ->
                fontDir.walk()
                    .filter { it.extension.equals("json", true) }
                    .forEach { path ->
                        val font = Font.fromDisk(assetsDir, path)
                        map[font.id] = font
                    }
            }
    }
    
    @PackTask(runAfter = ["FontContent#discoverAllFonts"])
    private fun write() {
        _customFonts.values.forEach { it.write(ASSETS_DIR) }
    }
    
}