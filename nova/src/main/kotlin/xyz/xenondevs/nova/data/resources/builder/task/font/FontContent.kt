package xyz.xenondevs.nova.data.resources.builder.task.font

import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder.Companion.ASSETS_DIR
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder.Companion.MCASSETS_ASSETS_DIR
import xyz.xenondevs.nova.data.resources.builder.font.Font
import xyz.xenondevs.nova.data.resources.builder.task.BuildStage
import xyz.xenondevs.nova.data.resources.builder.task.PackTask
import xyz.xenondevs.nova.data.resources.builder.task.PackTaskHolder
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.notExists
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
    
    fun add(font: Font){
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
    
    @PackTask(stage = BuildStage.POST_EXTRACT_ASSETS)
    private fun discoverAllFonts() {
        discoverFonts(MCASSETS_ASSETS_DIR, MCASSETS_ASSETS_DIR.resolve("minecraft/font/"), _vanillaFonts)
        discoverFonts(ASSETS_DIR, ASSETS_DIR.resolve("minecraft/font/"), _customFonts)
        builder.assetPacks.forEach { discoverFonts(ASSETS_DIR, ASSETS_DIR.resolve("${it.namespace}/font/"), _customFonts) }
    }
    
    private fun discoverFonts(assetsDir: Path, fontDir: Path, map: MutableMap<ResourcePath, Font>) {
        if (fontDir.notExists())
            return
        
        fontDir.walk()
            .filter { it.extension.equals("json", true) }
            .forEach { path -> 
                val font = Font.fromDisk(assetsDir, path)
                map[font.id] = font
            }
    }
    
    @PackTask(stage = BuildStage.LATE_WRITE)
    private fun write() {
        _customFonts.values.forEach { it.write(ASSETS_DIR) }
    }
    
}