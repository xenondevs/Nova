package xyz.xenondevs.nova.resources.builder.task

import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.util.data.readImage
import java.awt.image.BufferedImage

/**
 * Cached access for textures of the resource pack.
 */
class TextureContent(private val builder: ResourcePackBuilder) : PackBuildData {
    
    private val textureCache = HashMap<ResourcePath<ResourceType.PngFile>, BufferedImage>()
    
    /**
     * Gets the texture under [path].
     */
    fun getImage(path: ResourcePath<ResourceType.PngFile>): BufferedImage {
        return textureCache.getOrPut(path) { builder.findOrThrow(path).readImage() }
    }
    
    /**
     * Lazily gets the texture under [path].
     */
    fun getImageLazily(path: ResourcePath<ResourceType.PngFile>): Lazy<BufferedImage> {
        return lazy { getImage(path) }
    }
    
}