package xyz.xenondevs.nova.resources.builder.task

import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.util.data.readImage
import java.awt.image.BufferedImage

class TextureContent(private val builder: ResourcePackBuilder) : PackTaskHolder {
    
    private val textureCache = HashMap<ResourcePath<ResourceType.Texture>, BufferedImage>()
    
    fun getImage(path: ResourcePath<ResourceType.Texture>): BufferedImage {
        return textureCache.getOrPut(path) { builder.findOrThrow(path).readImage() }
    }
    
    fun getImageLazily(path: ResourcePath<ResourceType.Texture>): Lazy<BufferedImage> {
        return lazy { getImage(path) }
    }
    
}