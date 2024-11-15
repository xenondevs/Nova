package xyz.xenondevs.nova.resources.builder.task.font

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.task.PackTask
import xyz.xenondevs.nova.resources.lookup.ResourceLookups

private const val HEIGHT = 16
private const val ASCENT = 12

class TextureIconContent internal constructor(
    builder: ResourcePackBuilder
) : CustomFontContent(
    builder,
    "nova:texture_icons_%s",
    true
) {
    
    private val added = HashSet<ResourcePath<ResourceType.Texture>>()
    
    /**
     * Request additional icons to be added to the texture icon font.
     * Ids should be in the format `namespace:path`, i.e. `minecraft:item/stone_sword`.
     */
    fun addIcons(vararg ids: String) {
        for (id in ids) addIcon(ResourcePath.of(ResourceType.Texture, id))
    }
    
    /**
     * Request additional icons to be added to the texture icon font.
     * File extension should not be included in the path.
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
    
    @PackTask(runBefore = ["FontContent#write"])
    private fun write() {
        // default icons
        addIcons(
            "minecraft:item/wooden_sword", "minecraft:item/wooden_shovel", "minecraft:item/wooden_pickaxe", "minecraft:item/wooden_axe", "minecraft:item/wooden_hoe",
            "minecraft:item/stone_sword", "minecraft:item/stone_shovel", "minecraft:item/stone_pickaxe", "minecraft:item/stone_axe", "minecraft:item/stone_hoe",
            "minecraft:item/iron_sword", "minecraft:item/iron_shovel", "minecraft:item/iron_pickaxe", "minecraft:item/iron_axe", "minecraft:item/iron_hoe",
            "minecraft:item/golden_sword", "minecraft:item/golden_shovel", "minecraft:item/golden_pickaxe", "minecraft:item/golden_axe", "minecraft:item/golden_hoe",
            "minecraft:item/diamond_sword", "minecraft:item/diamond_shovel", "minecraft:item/diamond_pickaxe", "minecraft:item/diamond_axe", "minecraft:item/diamond_hoe",
            "minecraft:item/netherite_sword", "minecraft:item/netherite_shovel", "minecraft:item/netherite_pickaxe", "minecraft:item/netherite_axe", "minecraft:item/netherite_hoe",
            "minecraft:item/shears"
        )
        
        ResourceLookups.TEXTURE_ICON_LOOKUP.set(fontCharLookup)
    }
    
    companion object {
        
        fun getIcon(id: Key): FontChar? =
            ResourceLookups.TEXTURE_ICON_LOOKUP[id]
        
    }
    
}
