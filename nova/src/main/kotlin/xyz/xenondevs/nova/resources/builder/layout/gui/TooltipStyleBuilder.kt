package xyz.xenondevs.nova.resources.builder.layout.gui

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.data.GuiSpriteMcMeta

internal class TooltipStyleLayout(
    val backgroundPath: ResourcePath<ResourceType.TooltipBackgroundTexture>,
    val backgroundMeta: GuiSpriteMcMeta?,
    val framePath: ResourcePath<ResourceType.TooltipFrameTexture>,
    val frameMeta: GuiSpriteMcMeta?
)

@RegistryElementBuilderDsl
class TooltipStyleBuilder internal constructor(
    id: Key,
    val resourcePackBuilder: ResourcePackBuilder
) {
    
    private var backgroundMeta: GuiSpriteMcMeta? = null
    private var frameMeta: GuiSpriteMcMeta? = null
    
    /**
     * The path to the background texture.
     */
    val backgroundPath = ResourcePath.of(ResourceType.TooltipBackgroundTexture, id)
    
    /**
     * The path to the frame texture.
     */
    val framePath = ResourcePath.of(ResourceType.TooltipFrameTexture, id)
    
    /**
     * Configures both [backgroundMeta] and [frameMeta] at once.
     */
    fun meta(meta: GuiSpriteMetaBuilder.() -> Unit) {
        backgroundMeta(meta)
        frameMeta(meta)
    }
    
    /**
     * Configures the mcmeta for the background texture.
     * Can be left unspecified to use the mcmeta from assets.
     */
    fun backgroundMeta(meta: GuiSpriteMetaBuilder.() -> Unit) {
        backgroundMeta = GuiSpriteMetaBuilder(resourcePackBuilder).apply(meta).build()
    }
    
    /**
     * Configures the mcmeta for the frame texture.
     * Can be left unspecified to use the mcmeta from assets.
     */
    fun frameMeta(meta: GuiSpriteMetaBuilder.() -> Unit) {
        frameMeta = GuiSpriteMetaBuilder(resourcePackBuilder).apply(meta).build()
    }
    
    internal fun build() =
        TooltipStyleLayout(backgroundPath, backgroundMeta, framePath, frameMeta)
    
}