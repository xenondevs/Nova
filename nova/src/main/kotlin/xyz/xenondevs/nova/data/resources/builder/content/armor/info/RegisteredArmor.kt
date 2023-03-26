package xyz.xenondevs.nova.data.resources.builder.content.armor.info

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.data.resources.ResourcePath

@JvmInline
internal value class ArmorTexturePath(val resourcePath: ResourcePath)

@JvmInline
internal value class ArmorEmissivityMapPath(val resourcePath: ResourcePath)

internal data class RegisteredArmor(
    val id: ResourceLocation,
    val layer1: ArmorTexturePath?,
    val layer2: ArmorTexturePath?,
    val layer1EmissivityMap: ArmorEmissivityMapPath?,
    val layer2EmissivityMap: ArmorEmissivityMapPath?,
    val interpolationMode: InterpolationMode,
    val fps: Double
) {
    
    enum class InterpolationMode {
        NONE,
        LINEAR
    }
    
}