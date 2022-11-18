package xyz.xenondevs.nova.data.resources.builder.content.armor

import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.builder.AssetPack
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.builder.content.PackContent
import xyz.xenondevs.nova.data.resources.builder.content.armor.info.ArmorTexture
import xyz.xenondevs.nova.data.resources.builder.content.armor.info.RegisteredArmor
import xyz.xenondevs.nova.util.intValue
import xyz.xenondevs.nova.util.isNotNullOrEmpty
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.roundToInt

private val EMPTY_TEXTURE = BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB)

internal class ArmorContent : PackContent {
    
    private val armor = HashMap<NamespacedId, Pair<Int, RegisteredArmor>>()
    private val textures = HashMap<RegisteredArmor, Array<List<BufferedImage>?>>()
    private val emissivityMaps = HashMap<RegisteredArmor, Array<List<BufferedImage>?>>()
    
    private var currentColor = 1
    
    override fun addFromPack(pack: AssetPack) {
        pack.armorIndex?.forEach { armor ->
            this.armor[armor.id] = nextColor() to armor
            
            textures[armor] = arrayOf(
                armor.layer1?.let { extractFrames(it.resourcePath) },
                armor.layer2?.let { extractFrames(it.resourcePath) }
            )
            
            emissivityMaps[armor] = arrayOf(
                armor.layer1EmissivityMap?.let { extractFrames(it.resourcePath) },
                armor.layer2EmissivityMap?.let { extractFrames(it.resourcePath) }
            )
        }
    }
    
    private fun nextColor(): Int {
        check(currentColor <= 0xFFFFFF)
        val color = currentColor
        currentColor += 2 // only use odd color values and leave the rest for vanilla armor colors
        return color or (0xFF shl 24)
    }
    
    override fun write() {
        writeLeatherArmorAtlas()
        writeMCPatcherArmor()
        
        Resources.updateArmorDataLookup(armor.entries.associateTo(HashMap()) { it.key to ArmorTexture(it.value.first) })
    }
    
    private fun writeLeatherArmorAtlas() {
        val layer1File = File(ResourcePackBuilder.ASSETS_DIR, "minecraft/textures/models/armor/leather_layer_1.png")
        val layer2File = File(ResourcePackBuilder.ASSETS_DIR, "minecraft/textures/models/armor/leather_layer_2.png")
        
        val layer1 = buildTexture(ImageIO.read(layer1File), 0)
        val layer2 = buildTexture(ImageIO.read(layer2File), 1)
        
        ImageIO.write(layer1, "PNG", layer1File)
        ImageIO.write(layer2, "PNG", layer2File)
    }
    
    // Nova uses a modified version of the "Fancy Pants" shader by Ancientkingg: https://github.com/Ancientkingg/fancyPants
    private fun buildTexture(defaultLayer: BufferedImage, layer: Int): BufferedImage {
        //<editor-fold desc="loading and creating empty texture", defaultstate="collapsed">
        // calculate width and height of the individual textures
        val texRes = max(textures.values.mapNotNull { it[layer] }.maxOfOrNull { images -> images.maxOf { it.width / 4 } } ?: 0, 16)
        val width = texRes * 4
        val height = texRes * 2
        
        // calculate width and height of the leather armor texture
        val totalWidth = (armor.values.sumOf { (if (emissivityMaps[it.second]!![layer] != null) 2 else 1) as Int } + 1) * width
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
            val textureFrames = textures[armor]!![layer]
            val emissivityMapFrames = emissivityMaps[armor]!![layer]
            
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
                    Color(textureFrames.size, (armor.fps * 24).roundToInt(), armor.interpolationMode.ordinal)
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
    
    private fun writeMCPatcherArmor() {
        armor.forEach { id, (color, armor) ->
            val citDir = File(ResourcePackBuilder.ASSETS_DIR, "minecraft/optifine/cit/${id.namespace}/armor/${id.name}/")
                .apply(File::mkdirs)
            val animDir = File(ResourcePackBuilder.ASSETS_DIR, "minecraft/optifine/anim/${id.namespace}/armor/${id.name}/")
                .apply(File::mkdirs)
            
            // write properties file
            val armorPropertiesFile = File(citDir, "${id.name}.properties")
            val armorProperties = HashMap<String, Any>()
            armorProperties["type"] = "armor"
            armorProperties["items"] = "leather_helmet leather_chestplate leather_leggings leather_boots"
            armorProperties["weight"] = "1"
            armorProperties["nbt.display.color"] = color
            armorProperties["texture.leather_layer_1"] = "layer_1"
            armorProperties["texture.leather_layer_1_overlay"] = "layer_1"
            armorProperties["texture.leather_layer_2"] = "layer_2"
            armorProperties["texture.leather_layer_2_overlay"] = "layer_2"
            armorPropertiesFile.writeText(armorProperties.entries.joinToString("\n") { it.key + "=" + it.value })
            armorPropertiesFile.writeProperties(armorProperties)
            
            // get textures
            val layer1Frames = textures[armor]!![0]
            val layer2Frames = textures[armor]!![1]
            
            // write textures
            val layer1 = layer1Frames?.get(0) // [armor] [layer (0)] [frame (0)]
            val layer1File = File(citDir, "layer_1.png")
            ImageIO.write(layer1 ?: EMPTY_TEXTURE, "PNG", layer1File)
            
            val layer2 = layer2Frames?.get(0) // [armor] [layer (1)] [frame (0)]
            val layer2File = File(citDir, "layer_2.png")
            ImageIO.write(layer2 ?: EMPTY_TEXTURE, "PNG", layer2File)
            
            // TODO: Drop frames if frame rate above 20 fps to prevent slowing down the animation
            // write texture animations
            fun writeAnimatedTexture(frames: List<BufferedImage>, layer: Int) {
                val animatedTexture = BufferedImage(frames[0].width, frames[0].height * frames.size, BufferedImage.TYPE_INT_ARGB)
                val graphics = animatedTexture.createGraphics()
                frames.forEachIndexed { idx, img -> graphics.drawImage(img, 0, img.height * idx, img.width, img.height, null) }
                graphics.dispose()
                ImageIO.write(animatedTexture, "PNG", File(animDir, "layer_$layer.png"))
                
                val animationPropertiesFile = File(animDir, "layer_$layer.properties")
                val animationProperties = HashMap<String, Any>()
                animationProperties["from"] = "./layer_$layer.png"
                animationProperties["to"] = "optifine/cit/${id.namespace}/armor/${id.name}/layer_$layer.png"
                animationProperties["x"] = "0"
                animationProperties["y"] = "0"
                animationProperties["w"] = frames[0].width
                animationProperties["h"] = frames[0].height
                animationProperties["duration"] = max((20 / armor.fps).roundToInt(), 1)
                animationProperties["interpolate"] = armor.interpolationMode != RegisteredArmor.InterpolationMode.NONE
                animationPropertiesFile.writeProperties(animationProperties)
            }
            
            if (layer1Frames != null && layer1Frames.size > 1)
                writeAnimatedTexture(layer1Frames, 1)
            if (layer2Frames != null && layer2Frames.size > 1)
                writeAnimatedTexture(layer2Frames, 2)
            
        }
    }
    
    private fun File.writeProperties(properties: Map<String, Any>) {
        writeText(properties.entries.joinToString("\n") { (key, value) -> "$key=$value" })
    }
    
}