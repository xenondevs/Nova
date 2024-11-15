package xyz.xenondevs.nova.resources.builder.task

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.commons.collections.eachRepeated
import xyz.xenondevs.commons.collections.repeated
import xyz.xenondevs.commons.collections.takeUnlessEmpty
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.model.EquipmentModel
import xyz.xenondevs.nova.resources.layout.equipment.AnimatedEquipmentLayout
import xyz.xenondevs.nova.resources.layout.equipment.AnimatedEquipmentLayout.Animation
import xyz.xenondevs.nova.resources.layout.equipment.InterpolationMode
import xyz.xenondevs.nova.resources.layout.equipment.StaticEquipmentLayout
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.util.MathUtils
import xyz.xenondevs.nova.util.data.ImageUtils
import xyz.xenondevs.nova.util.toResourceLocation
import xyz.xenondevs.nova.util.toResourcePath
import java.awt.image.BufferedImage

internal class RuntimeEquipmentData(
    val textureFrames: List<ResourceLocation>?,
    val cameraOverlayFrames: List<ResourceLocation>?
) {
    
    init {
        require(textureFrames == null || textureFrames.isNotEmpty()) { "Texture frames must not be empty" }
        require(cameraOverlayFrames == null || cameraOverlayFrames.isNotEmpty()) { "Camera overlay frames must not be empty" }
    }
    
    val isAnimated: Boolean =
        textureFrames != null && textureFrames.size > 1 || cameraOverlayFrames != null && cameraOverlayFrames.size > 1
    
}

// TODO: Non-interpolated animations can be optimized to not create one equipment model for each tick
// TODO: emissivity maps and textures only used for generation can be deleted from final resource pack
// fixme: duplicate images written to pack for same interpolation transitions
class EquipmentContent internal constructor(private val builder: ResourcePackBuilder) : PackTaskHolder {
    
    private val textureContent by builder.getHolderLazily<TextureContent>()
    
    /**
     * Caches interpolated textures.
     * Format: Map<Pair<FromFile, ToFile>, Map<Blend 0-255, InterpolatedImage>>
     */
    private val interpolationCache = HashMap<Pair<ResourcePath<ResourceType.Texture>, ResourcePath<ResourceType.Texture>>, Int2ObjectMap<BufferedImage>>()
    private var generatedCount = 0
    
    @PackTask(
        stage = BuildStage.PRE_WORLD,
        runAfter = ["ExtractTask#extractAll"]
    )
    private fun write() {
        ResourceLookups.EQUIPMENT = NovaRegistries.EQUIPMENT.associateWith { equipment ->
            when (val layout = equipment.makeLayout(builder)) {
                is StaticEquipmentLayout -> generatedStaticEquipmentModel(equipment.id.toResourcePath(ResourceType.Equipment), layout)
                is AnimatedEquipmentLayout -> generateAnimatedEquipmentModel(equipment.id.toResourcePath(ResourceType.Equipment), layout)
            }
        }
    }
    
    private fun generatedStaticEquipmentModel(id: ResourcePath<ResourceType.Equipment>, layout: StaticEquipmentLayout): RuntimeEquipmentData {
        for ((equipmentType, layers) in layout.types) {
            for(layer in layers) {
                if (layer.emissivityMap != null) {
                    applyEmissivityMap(layer.texture, layer.emissivityMap, "textures/entity/equipment/$equipmentType/")
                }
            }
        }
        
        val model = layout.toEquipmentModel()
        validateEquipmentModel(model)
        
        builder.writeJson(id, model)
        
        return RuntimeEquipmentData(
            listOf(id.toResourceLocation()),
            layout.cameraOverlay?.let { listOf(it.toResourceLocation()) }
        )
    }
    
    private fun generateAnimatedEquipmentModel(id: ResourcePath<ResourceType.Equipment>, layout: AnimatedEquipmentLayout): RuntimeEquipmentData {
        val textureFrames = generateAnimatedEquipmentTexture(id, layout)
        val cameraOverlayFrames = generateAnimatedCameraOverlay(layout)
        return RuntimeEquipmentData(textureFrames, cameraOverlayFrames)
    }
    
    private fun generateAnimatedCameraOverlay(layout: AnimatedEquipmentLayout): List<ResourceLocation>? {
        val cameraOverlay = layout.cameraOverlay
        val cameraOverlayFrameCount: Int
        val cameraOverlayFrames: List<ResourcePath<ResourceType.Texture>>
        if (cameraOverlay != null) {
            cameraOverlayFrameCount = cameraOverlay.frames.size * cameraOverlay.ticksPerFrame
            cameraOverlayFrames = generateTextureAnimation(cameraOverlay, ResourceType.Texture)
        } else {
            cameraOverlayFrameCount = 0
            cameraOverlayFrames = emptyList()
        }
        
        return cameraOverlayFrames.map(ResourcePath<*>::toResourceLocation).takeUnlessEmpty()
    }
    
    private fun generateAnimatedEquipmentTexture(id: ResourcePath<ResourceType.Equipment>, layout: AnimatedEquipmentLayout): List<ResourceLocation> {
        // generate all animations (merge with emissivity map, apply interpolation)
        // [equipmentType][layer][frame]
        val animations: Map<EquipmentModel.Type, List<List<ResourcePath<ResourceType.EquipmentTexture>>>> =
            layout.types.mapValues { (_, layers) -> layers.map(::generateLayerAnimation) }
        
        // find the total frame count needed to display all animations using a single frame number
        // (least common multiple of all frame counts)
        val textureFrameCount: Int = animations.values.asSequence().flatten()
            .fold(1) { acc, layer -> MathUtils.lcm(acc, layer.size) }
        
        val textureFrames = ArrayList<ResourcePath<ResourceType.Equipment>>(textureFrameCount)
        repeat(textureFrameCount) { frame ->
            val path = ResourcePath(ResourceType.Equipment, id.namespace, id.path + "_$frame")
            textureFrames += path
            
            val equipmentModelForFrame = EquipmentModel(
                layout.types.mapValues { (equipmentType, layers) ->
                    layers.withIndex().map { (layerIdx, layer) ->
                        val layerFrames = animations[equipmentType]!![layerIdx]
                        val texture = layerFrames[frame % layerFrames.size]
                        EquipmentModel.Layer(texture, false, layer.dyeable)
                    }
                }
            )
            
            builder.writeJson(path, equipmentModelForFrame)
        }
        
        return textureFrames.map(ResourcePath<*>::toResourceLocation)
    }
    
    /**
     * Generates/collects the textures required for animated equipment layers (texture and emissivity map), then returns
     * the sequence of [ResourcePaths][ResourcePath] pointing to the (generated) textures.
     */
    private fun <T : ResourceType.EquipmentTexture> generateLayerAnimation(layer: AnimatedEquipmentLayout.Layer<T>): List<ResourcePath<T>> {
        if (layer.emissivityMap != null) {
            var textureImages = generateTextureAnimationImages(layer.texture)
            var emissivityMapImages = generateTextureAnimationImages(layer.emissivityMap)
            
            val frameCount = MathUtils.lcm(textureImages.size, emissivityMapImages.size)
            textureImages = textureImages.repeated(frameCount / textureImages.size).map(ImageUtils::copyToARGB) // conversion to ARGB is important to support alpha in image!
            emissivityMapImages = emissivityMapImages.repeated(frameCount / emissivityMapImages.size).map(ImageUtils::copyToARGB)
            
            applyEmissivityMaps(textureImages, emissivityMapImages)
            return writeImages(textureImages, layer.resourceType)
        } else {
            return generateTextureAnimation(layer.texture, layer.resourceType)
        }
    }
    
    
    /**
     * Applies each emissivity map from [emissivityMapFrames] to the corresponding texture in [textureFrames].
     */
    private fun applyEmissivityMaps(
        textureFrames: List<BufferedImage>,
        emissivityMapFrames: List<BufferedImage>
    ) {
        require(textureFrames.size == emissivityMapFrames.size) { "Texture and emissivity map frame counts don't match" }
        
        val emissiveFrames = ArrayList<BufferedImage>()
        for (frame in textureFrames.indices) {
            val texture = textureFrames[frame]
            val emissivityMap = emissivityMapFrames[frame]
            applyEmissivityMap(texture, emissivityMap)
            emissiveFrames += texture
        }
    }
    
    /**
     * Applies the emissivity map from [emissivityMap] to the texture under [texture] in [textureDir].
     */
    private fun applyEmissivityMap(texture: ResourcePath<ResourceType.Texture>, emissivityMap: ResourcePath<ResourceType.Texture>, textureDir: String) {
        var textureImage = ImageUtils.copyToARGB(textureContent.getImage(texture))
        val emissivityMapImage = textureContent.getImage(emissivityMap)
        
        applyEmissivityMap(textureImage, emissivityMapImage)
        builder.writeImage(texture, textureImage)
    }
    
    /**
     * Applies the [emissivityMap] to [texture].
     */
    private fun applyEmissivityMap(texture: BufferedImage, emissivityMap: BufferedImage) {
        require(texture.width == emissivityMap.width && texture.height == emissivityMap.height) { "Emissivity map must have the same dimensions as the texture" }
        
        for (x in 0 until texture.width) {
            for (y in 0 until texture.height) {
                val rgb = texture.getRGB(x, y)
                if (rgb shr 24 == 0)
                    continue // skip transparent pixels
                
                val red = rgb shr 16 and 0xFF
                val green = rgb shr 8 and 0xFF
                val blue = rgb and 0xFF
                
                // expected to be black/white, so we can just use any channel
                val emissivity = emissivityMap.getRGB(x, y) and 0xFF
                val alpha = (255 - emissivity).coerceAtLeast(1)

                texture.setRGB(x, y, (alpha shl 24) or (red shl 16) or (green shl 8) or blue)
            }
        }
    }
    
    /**
     * Generates/collects the textures required for [animation], then returns the sequence of [ResourcePaths][ResourcePath]
     * pointing to the (generated) textures.
     */
    private fun <T : ResourceType.Texture> generateTextureAnimation(animation: Animation<T>, location: T): List<ResourcePath<T>> {
        val (keyFrames, ticksPerFrame, interpolationMode) = animation
        if (interpolationMode == InterpolationMode.NONE) {
            return keyFrames.eachRepeated(ticksPerFrame)
        }
        
        val frames = ArrayList<ResourcePath<T>>(keyFrames.size * ticksPerFrame)
        for ((keyFrameId, keyFrame) in keyFrames.withIndex()) {
            frames += keyFrame
            frames += writeImages(
                generateInterpolatedImages(
                    keyFrame, keyFrames[(keyFrameId + 1) % keyFrames.size],
                    ticksPerFrame - 1
                ),
                location
            )
        }
        
        return frames
    }
    
    /**
     * Generates a sequence of [BufferedImages][BufferedImage] representing the [animation] by interpolating between the key frames.
     */
    private fun generateTextureAnimationImages(animation: Animation<*>): List<BufferedImage> {
        val (keyFrames, ticksPerFrame, interpolationMode) = animation
        
        val keyFrameImages = keyFrames.map { textureContent.getImage(it) }
        if (interpolationMode == InterpolationMode.NONE)
            return keyFrameImages.eachRepeated(ticksPerFrame).map(ImageUtils::copyToARGB)
        
        val frames = ArrayList<BufferedImage>(keyFrames.size * ticksPerFrame)
        for ((keyFrameId, keyFrame) in keyFrames.withIndex()) {
            frames += textureContent.getImage(keyFrame)
            
            frames += generateInterpolatedImages(
                keyFrame, keyFrames[(keyFrameId + 1) % keyFrames.size],
                ticksPerFrame - 1
            )
        }
        
        return frames
    }
    
    /**
     * Generates [count] interpolated images between [from] and [to].
     */
    private fun generateInterpolatedImages(
        from: ResourcePath<ResourceType.Texture>,
        to: ResourcePath<ResourceType.Texture>, 
        count: Int
    ): List<BufferedImage> {
        if (count <= 0)
            return emptyList()
        
        val fromImage by textureContent.getImageLazily(from)
        val toImage by textureContent.getImageLazily(to)
        
        val interpolatedImages = ArrayList<BufferedImage>()
        for (frame in 0..<count) {
            val progress = frame * 255 / count
            interpolatedImages += interpolationCache
                .getOrPut(from to to, ::Int2ObjectOpenHashMap)
                .getOrPut(progress) { ImageUtils.lerp(fromImage, toImage, progress / 255f) }
        }
        
        return interpolatedImages
    }
    
    /**
     * Writes the [images] to the texture directory and returns the [ResourcePaths][ResourcePath]
     * pointing to the written images.
     */
    private fun <T : ResourceType.Texture> writeImages(images: List<BufferedImage>, location: T): List<ResourcePath<T>> =
        images.map { writeImage(it, location) }
    
    /**
     * Writes the [image] to the texture directory and returns the [ResourcePath] pointing to the written image.
     */
    private fun <T : ResourceType.Texture> writeImage(image: BufferedImage, location: T): ResourcePath<T> {
        val path = ResourcePath(location, "nova", "generated/equipment_${generatedCount++}")
        builder.writeImage(path, image)
        return path
    }
    
    /**
     * Checks whether all textures referenced in [model] exist and throws an exception if not.
     */
    private fun validateEquipmentModel(model: EquipmentModel) {
        for ((_, layers) in model.layers) {
            for (layer in layers) {
                builder.findOrThrow(layer.texture)
            }
        }
    }
    
}