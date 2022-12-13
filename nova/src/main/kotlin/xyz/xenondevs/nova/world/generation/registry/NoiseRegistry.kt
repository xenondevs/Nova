package xyz.xenondevs.nova.world.generation.registry

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings
import net.minecraft.world.level.levelgen.synth.NormalNoise.NoiseParameters
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId

object NoiseRegistry : WorldGenRegistry() {
    
    override val neededRegistries = setOf(Registry.NOISE_REGISTRY, Registry.NOISE_GENERATOR_SETTINGS_REGISTRY)
    
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
    
    override fun register(registryAccess: RegistryAccess) {
        loadFiles("noise", NoiseParameters.CODEC, noiseParameters)
        registerAll(registryAccess, Registry.NOISE_REGISTRY, noiseParameters)
        loadFiles("noise_settings", NoiseGeneratorSettings.CODEC, noiseGenerationSettings)
        registerAll(registryAccess, Registry.NOISE_GENERATOR_SETTINGS_REGISTRY, noiseGenerationSettings)
    }
}