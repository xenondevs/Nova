package xyz.xenondevs.nova.data.resources.builder.content.armor

import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.builder.AssetPack
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.builder.content.PackContent
import xyz.xenondevs.nova.data.resources.builder.content.armor.info.ArmorEmissivityMapPath
import xyz.xenondevs.nova.data.resources.builder.content.armor.info.ArmorTexture
import xyz.xenondevs.nova.data.resources.builder.content.armor.info.ArmorTexturePath
import xyz.xenondevs.nova.data.resources.builder.content.armor.info.RegisteredArmor
import xyz.xenondevs.nova.util.intValue
import xyz.xenondevs.nova.util.isNotNullOrEmpty
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max

internal class ArmorContent : PackContent {
    
    private val armor = HashMap<NamespacedId, Pair<Int, RegisteredArmor>>()
    private var currentColor = 1
    
    override fun addFromPack(pack: AssetPack) {
        pack.armorIndex?.forEach { armor[it.id] = nextColor() to it }
    }
    
    private fun nextColor(): Int {
        check(currentColor <= 0xFFFFFF)
        val color = currentColor
        currentColor += 2 // only use odd color values and leave the rest for vanilla armor colors
        return color or (0xFF shl 24)
    }
    
    override fun write() {
        val layer1File = File(ResourcePackBuilder.ASSETS_DIR, "minecraft/textures/models/armor/leather_layer_1.png")
        val layer2File = File(ResourcePackBuilder.ASSETS_DIR, "minecraft/textures/models/armor/leather_layer_2.png")
        
        val layer1 = buildTexture(ImageIO.read(layer1File)) { it.layer1 to it.layer1EmissivityMap }
        val layer2 = buildTexture(ImageIO.read(layer2File)) { it.layer2 to it.layer2EmissivityMap }
        
        ImageIO.write(layer1, "PNG", layer1File)
        ImageIO.write(layer2, "PNG", layer2File)
        
        Resources.updateArmorDataLookup(armor.entries.associateTo(HashMap()) { it.key.toString() to ArmorTexture(it.value.first) })
    }
    
    // Nova uses a modified version of the "Fancy Pants" shader by Ancientkingg: https://github.com/Ancientkingg/fancyPants
    private fun buildTexture(defaultLayer: BufferedImage, layerReceiver: (RegisteredArmor) -> Pair<ArmorTexturePath?, ArmorEmissivityMapPath?>): BufferedImage {
        val textures = HashMap<RegisteredArmor, List<BufferedImage>?>()
        val emissivityMaps = HashMap<RegisteredArmor, List<BufferedImage>?>()
        
        //<editor-fold desc="loading and creating empty texture", defaultstate="collapsed">
        // load all textures
        armor.forEach { _, (_, armor) ->
            val (texture, emissivityMap) = layerReceiver(armor)
            textures[armor] = texture?.let { extractFrames(it.resourcePath) }
            emissivityMaps[armor] = emissivityMap?.let { extractFrames(it.resourcePath) }
        }
        
        // calculate width and height of the individual textures
        val texRes = max(textures.values.filterNotNull().maxOfOrNull { images -> images.maxOf { it.width / 4 } } ?: 0, 16)
        val width = texRes * 4
        val height = texRes * 2
        
        // calculate width and height of the leather armor texture
        val totalWidth = (armor.values.sumOf { (if (layerReceiver(it.second).second != null) 2 else 1) as Int } + 1) * width
        val totalHeight = max(textures.values.filterNotNull().maxOfOrNull { it.size * height } ?: 0, height)
        
        // create texture image
        val texture = BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_ARGB)
        val graphics = texture.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        //</editor-fold>
        
        // draw textures to texture image
        graphics.drawImage(defaultLayer, 0, 0, width, height, null)
        var textureIdx = 1
        armor.forEach { _, (color, armor) ->
            val textureFrames = textures[armor]
            val emissivityMapFrames = emissivityMaps[armor]
            
            // draw texture frames
            textureFrames?.forEachIndexed { frameIdx, frame ->
                graphics.drawImage(frame, textureIdx * width, frameIdx * height, width, height, null)
            }
            
            //<editor-fold desc="armor texture metadata">
            // color marker
            texture.setRGB(textureIdx * width, 0, color)
    
            // animation marker
            val animationMarker =
                // rgb(frame amount, speed, interpolation)
                if (textureFrames != null && textureFrames.size > 1) {
                    Color(textureFrames.size, armor.animationSpeed, armor.interpolationMode.ordinal)
                } else Color(0, 0, 0, 0)
            texture.setRGB(textureIdx * width + 1, 0, animationMarker.rgb)
    
            // other properties (tint, emissivity)
            val miscPropertiesMarker =
                // rgb(emissivity (0: off, 1: partial, > 1: full), tint, N/A)
                Color(emissivityMapFrames.isNotNullOrEmpty().intValue, 0, 0)
            texture.setRGB(textureIdx * width + 2, 0, miscPropertiesMarker.rgb)
            //</editor-fold>
            
            textureIdx++
            
            // draw emissivity map frames
            if (emissivityMapFrames.isNotNullOrEmpty()) {
                emissivityMapFrames.forEachIndexed { frameIdx, frame ->
                    graphics.drawImage(frame, textureIdx * width, frameIdx * height, width, height, null)
                }
                textureIdx++
            }
        }
        
        // marks the texture as a leather layer
        texture.setRGB(0, 1, Color(255, 255, 255, 255).rgb)
        // stores the texture resolution
        texture.setRGB(1, 0, texRes)
        
        graphics.dispose()
        return texture
    }
    
    private fun extractFrames(resourcePath: ResourcePath): List<BufferedImage> {
        val file = resourcePath.getFile(ResourcePackBuilder.ASSETS_DIR, "textures", "png")
        require(file.exists()) { "Armor file does not exist: $file" }
        
        val image = ImageIO.read(file)
        
        val width = image.width // by default: 64
        val height = image.height // by default: 32
        
        val frameHeight = width / 2
        val frameAmount = height / frameHeight
        
        val frames = ArrayList<BufferedImage>()
        repeat(frameAmount) {
            frames += image.getSubimage(0, it * frameHeight, width, frameHeight)
        }
        
        return frames
    }
    
}