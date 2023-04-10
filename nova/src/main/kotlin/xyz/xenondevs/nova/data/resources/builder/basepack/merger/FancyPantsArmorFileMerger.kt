package xyz.xenondevs.nova.data.resources.builder.basepack.merger

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.resources.builder.basepack.BasePacks
import xyz.xenondevs.nova.data.resources.builder.content.armor.ArmorData
import xyz.xenondevs.nova.data.resources.builder.content.armor.info.RegisteredArmor.InterpolationMode
import xyz.xenondevs.nova.util.data.readImage
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString

private const val LAYER_1 = "assets/minecraft/textures/models/armor/leather_layer_1.png"
private const val LAYER_2 = "assets/minecraft/textures/models/armor/leather_layer_2.png"

private const val TEXTURE_WIDTH = 64
private const val TEXTURE_HEIGHT = 32

/**
 * Merges custom armor from the [Fancy Pants shader](https://github.com/Ancientkingg/fancyPants).
 */
internal class FancyPantsArmorFileMerger(basePacks: BasePacks) : FileMerger(basePacks) {
    
    override fun acceptsFile(relPath: Path): Boolean {
        val relPathStr = relPath.invariantSeparatorsPathString
        return relPathStr == LAYER_1 || relPathStr == LAYER_2
    }
    
    // TODO: Add support for:
    //  - "full" emissivity
    //  - tint
    //  - different frame rates for layers of the same armor
    override fun merge(source: Path, destination: Path, relPath: Path) {
        val image = source.readImage()
        
        val width = image.width
        val height = image.height
        
        // check dimensions
        if (width % TEXTURE_WIDTH != 0 || height % TEXTURE_HEIGHT != 0) {
            LOGGER.warning("$source has invalid dimensions. Expected $TEXTURE_WIDTH x $TEXTURE_HEIGHT or multiples of those, got $width x $height.")
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
        val armorSections = width / TEXTURE_WIDTH
        
        var frameAmount = 0
        var currentArmor: ArmorData? = null
        
        // loop over all armor sections
        for (armorSection in 0 until armorSections) {
            val sectionImage = image.getSubimage(armorSection * TEXTURE_WIDTH, 0, TEXTURE_WIDTH, height)
            
            if (currentArmor == null) { // new armor
                var fps = 0.0
                var interpolationMode: InterpolationMode = InterpolationMode.NONE
                var frames: List<BufferedImage>
                val color = image.getRGB(armorSection * TEXTURE_WIDTH, 0) and 0xFFFFFF
                val animationMarker = image.getRGB(armorSection * TEXTURE_WIDTH + 1, 0) // rgb(frameAmount, speed, interpolate)
                val extraProperties = image.getRGB(armorSection * TEXTURE_WIDTH + 2, 0).asColor() // rgb(emissivity, tint, N/A)
                
                if (animationMarker != 0) {
                    val animationMarkerColor = animationMarker.asColor()
                    frameAmount = animationMarkerColor.red
                    frames = extractFrames(sectionImage, frameAmount)
                    fps = animationMarkerColor.green / 24.0
                    interpolationMode = if (animationMarkerColor.blue > 0) InterpolationMode.LINEAR else InterpolationMode.NONE
                } else {
                    frames = extractFrames(sectionImage, 1)
                    frameAmount = 1
                }
                
                val armor = registerArmor(color, layer, frames, interpolationMode, fps)
                // assign to currenArmor if there is an emissivity map in the next section
                if (extraProperties.red == 1)
                    currentArmor = armor
            } else { // emissivity map for armor
                val frames = extractFrames(sectionImage, frameAmount)
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
    
    private fun extractFrames(image: BufferedImage, amount: Int): List<BufferedImage> {
        val frames = ArrayList<BufferedImage>()
        repeat(amount) { frames += image.getSubimage(0, it * TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT) }
        return frames
    }
    
    private fun registerArmor(color: Int, layer: Int, frames: List<BufferedImage>, interpolationMode: InterpolationMode, fps: Double): ArmorData {
        val armor = basePacks.customArmor.getOrPut(color) {
            val colorObj = color.asColor()
            ArmorData(
                ResourceLocation("base_pack", "armor_${colorObj.red}_${colorObj.green}_${colorObj.blue}"),
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