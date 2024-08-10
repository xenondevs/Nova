package xyz.xenondevs.nova.resources.builder.basepack.merger

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.resources.builder.basepack.BasePacks
import xyz.xenondevs.nova.resources.builder.task.ArmorData
import xyz.xenondevs.nova.resources.layout.armor.InterpolationMode
import xyz.xenondevs.nova.util.data.readImage
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.readText

private const val LAYER_1 = "assets/minecraft/textures/models/armor/leather_layer_1.png"
private const val LAYER_2 = "assets/minecraft/textures/models/armor/leather_layer_2.png"

/**
 * Merges custom armor from the [Fancy Pants shader](https://github.com/Ancientkingg/fancyPants).
 */
internal class FancyPantsArmorFileMerger(basePacks: BasePacks) : FileMerger(basePacks) {
    
    override fun acceptsFile(relPath: Path): Boolean {
        val relPathStr = relPath.invariantSeparatorsPathString
        return relPathStr == LAYER_1 || relPathStr == LAYER_2
    }
    
    private fun findTextureResolution(baseDir: Path): Int? {
        val shaderFile = baseDir.resolve("assets/minecraft/shaders/core/rendertype_armor_cutout_no_cull.fsh")
        val text = shaderFile.readText()
        
        fun findDefinedProperty(name: String): Int? {
            val regex = Regex("""#define $name (\d+)""")
            return regex.find(text)?.groupValues?.get(1)?.toIntOrNull()
        }
        
        return findDefinedProperty("TEX_RES") // unobfuscated
            ?: findDefinedProperty("V1") // ItemsAdder 😔
    }
    
    // TODO: Add support for:
    //  - "full" emissivity
    //  - tint
    //  - different frame rates for layers of the same armor
    override fun merge(source: Path, destination: Path, baseDir: Path, relPath: Path) {
        val texRes = findTextureResolution(baseDir) ?: 16
        val textureWidth = texRes * 4
        val textureHeight = texRes * 2
        
        val image = source.readImage()
        
        val width = image.width
        val height = image.height
        
        // check dimensions
        if (width % textureWidth != 0 || height % textureHeight != 0) {
            LOGGER.warning("$source has invalid dimensions. Expected $textureWidth x $textureHeight or multiples of those, got $width x $height.")
            return
        }
        
        // rgba(255, 255, 255, 255) is -1 in 32-bit signed int
        if (image.getRGB(0, 1) != -1) {
            LOGGER.warning("Cannot read armor $source because it is missing the Fancy Pants texture marker. (rgba(255, 255, 255, 255) at x=0, y=1)")
            return
        }
        
        // determine texture layer
        val layer = determineLayer(relPath)
        
        // the amount of armor sections, might also be emissivity maps
        val armorSections = width / textureWidth
        
        var frameAmount = 0
        var currentArmor: ArmorData? = null
        
        // loop over all armor sections
        for (armorSection in 0..<armorSections) {
            val sectionImage = image.getSubimage(armorSection * textureWidth, 0, textureWidth, height)
            
            if (currentArmor == null) { // new armor
                var fps = 0.0
                var interpolationMode: InterpolationMode = InterpolationMode.NONE
                var frames: List<BufferedImage>
                val color = image.getRGB(armorSection * textureWidth, 0) and 0xFFFFFF
                val animationMarker = image.getRGB(armorSection * textureWidth + 1, 0) // rgb(frameAmount, speed, interpolate)
                val extraProperties = image.getRGB(armorSection * textureWidth + 2, 0).asColor() // rgb(emissivity, tint, N/A)
                
                if (animationMarker != 0) {
                    val animationMarkerColor = animationMarker.asColor()
                    frameAmount = animationMarkerColor.red
                    frames = extractFrames(textureWidth, textureHeight, sectionImage, frameAmount)
                    fps = animationMarkerColor.green / 24.0
                    interpolationMode = if (animationMarkerColor.blue > 0) InterpolationMode.LINEAR else InterpolationMode.NONE
                } else {
                    frames = extractFrames(textureWidth, textureHeight, sectionImage, 1)
                    frameAmount = 1
                }
                
                val armor = registerArmor(color, layer, frames, interpolationMode, fps)
                // assign to currenArmor if there is an emissivity map in the next section
                if (extraProperties.red == 1)
                    currentArmor = armor
            } else { // emissivity map for armor
                val frames = extractFrames(textureWidth, textureHeight, sectionImage, frameAmount)
                currentArmor.emissivityMapsLayers[layer] = frames
                currentArmor = null
            }
        }
        
    }
    
    private fun determineLayer(path: Path): Int =
        when (path.invariantSeparatorsPathString) {
            LAYER_1 -> 0
            LAYER_2 -> 1
            else -> throw IllegalArgumentException("Invalid path $path")
        }
    
    private fun extractFrames(textureWidth: Int, textureHeight: Int, image: BufferedImage, amount: Int): List<BufferedImage> {
        val frames = ArrayList<BufferedImage>()
        repeat(amount) { frames += image.getSubimage(0, it * textureHeight, textureWidth, textureHeight) }
        return frames
    }
    
    private fun registerArmor(color: Int, layer: Int, frames: List<BufferedImage>, interpolationMode: InterpolationMode, fps: Double): ArmorData {
        val armor = basePacks.customArmor.getOrPut(color) {
            val colorObj = color.asColor()
            ArmorData(
                ResourceLocation.fromNamespaceAndPath("base_pack", "armor_${colorObj.red}_${colorObj.green}_${colorObj.blue}"),
                color,
                arrayOfNulls(2),
                arrayOfNulls(2),
                interpolationMode,
                fps
            )
        }
        
        armor.textureLayers[layer] = frames
        
        return armor
    }
    
}

private fun Int.asColor(): Color = Color(this, true)