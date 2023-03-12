package xyz.xenondevs.nova.world.generation.registry

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings
import net.minecraft.world.level.levelgen.synth.NormalNoise.NoiseParameters
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.util.NMSUtils
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen

@ExperimentalWorldGen
object NoiseRegistry : WorldGenRegistry(NMSUtils.REGISTRY_ACCESS) {
    
    override val neededRegistries get() = setOf(Registries.NOISE, Registries.NOISE_SETTINGS)
    
    private val noiseParameters = Object2ObjectOpenHashMap<NamespacedId, NoiseParameters>()
    private val noiseGenerationSettings = Object2ObjectOpenHashMap<NamespacedId, NoiseGeneratorSettings>()
    
    fun registerNoiseParameters(addon: Addon, name: String, noiseParams: NoiseParameters) {
        val id = NamespacedId(addon, name)
        require(id !in noiseParameters) { "Duplicate noise parameters $id" }
        noiseParameters[id] = noiseParams
    }
    
    fun registerNoiseGenerationSettings(addon: Addon, name: String, noiseGenSettings: NoiseGeneratorSettings) {
        val id = NamespacedId(addon, name)
        require(id !in noiseGenerationSettings) { "Duplicate noise generation settings $id" }
        noiseGenerationSettings[id] = noiseGenSettings
    }
    
    override fun register() {
        loadFiles("noise", NoiseParameters.CODEC, noiseParameters)
        registerAll(Registries.NOISE, noiseParameters)
        loadFiles("noise_settings", NoiseGeneratorSettings.CODEC, noiseGenerationSettings)
        registerAll(Registries.NOISE_SETTINGS, noiseGenerationSettings)
    }
}