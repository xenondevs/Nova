package xyz.xenondevs.nova.resources.builder.task

import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.util.data.readImage
import java.awt.image.BufferedImage
import java.nio.file.Path

class TextureContent(private val builder: ResourcePackBuilder) : PackTaskHolder {
    
    private val textureCache = HashMap<Path, BufferedImage>()
    
    fun getImage(path: Path): BufferedImage {
        return textureCache.getOrPut(path) { path.readImage() }
    }
    
    fun getImageLazily(path: Path): Lazy<BufferedImage> {
        return lazy { getImage(path) }
    }
    
}