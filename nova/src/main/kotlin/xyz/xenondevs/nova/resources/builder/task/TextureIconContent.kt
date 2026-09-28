package xyz.xenondevs.nova.resources.builder.task

import net.kyori.adventure.key.Key
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.get
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.lookup.ResourceLookups

private const val HEIGHT = 16
private const val ASCENT = 12

/**
 * Allows requesting "texture icons", which are font characters that use textures as their glyphs
 * (i.e. tool textures like pickaxes).
 */
class TextureIconContent(
    builder: ResourcePackBuilder
) : CustomFontContent(
    builder,
    "nova:texture_icons_%s",
    1..19
), PackBuildData {
    
    private val added = HashSet<ResourcePath<ResourceType.Texture>>()
    
    /**
     * Request additional icons to be added to the texture icon font.
     * Ids should be in the format `namespace:path`, i.e. `minecraft:item/stone_sword`.
     * 
     * The corresponding icon character be resolved using [getIcon] after resource pack generation.
     */
    fun addIcons(vararg ids: String) {
        for (id in ids) addIcon(ResourcePath.of(ResourceType.Texture, id))
    }
    
    /**
     * Request additional icons to be added to the texture icon font.
     * File extension should not be included in the path.
     *
     * The corresponding icon character be resolved using [getIcon] after resource pack generation.
     */
    fun addIcons(ids: Iterable<ResourcePath<ResourceType.Texture>>) {
        for (id in ids) addIcon(id)
    }
    
    private fun addIcon(path: ResourcePath<ResourceType.Texture>) {
        if (path in added)
            return
        
        addEntry(path, path.toType(ResourceType.FontTexture), HEIGHT, ASCENT)
        added += path
    }
    
    /**
     * Writes the texture icons requested in [TextureIconContent] to [FontContent].
     */
    inner class Write : PackTask {
        
        override val runsBefore = setOf(FontContent.Write::class)
        
        override suspend fun run() {
            ResourceLookups.textureIcon = fontCharLookup
        }
        
    }
    
    companion object {
        
        /**
         * Gets a provider for the [FontChar] of the texture icon requested in [TextureIconContent],
         * where id is the path of texture, i.e. `minecraft:item/stone_sword`.
         */
        fun getIcon(id: Key): Provider<FontChar?> =
            ResourceLookups.textureIconLookup[id]
        
    }
    
}
