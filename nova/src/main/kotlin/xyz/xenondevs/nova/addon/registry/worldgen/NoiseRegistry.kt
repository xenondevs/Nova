package xyz.xenondevs.nova.addon.registry.worldgen

import it.unimi.dsi.fastutil.doubles.DoubleArrayList
import it.unimi.dsi.fastutil.doubles.DoubleList
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings
import net.minecraft.world.level.levelgen.synth.NormalNoise.NoiseParameters
import xyz.xenondevs.nova.addon.registry.AddonHolder
import xyz.xenondevs.nova.registry.vanilla.VanillaRegistries
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen

interface NoiseRegistry : AddonHolder {
    
    @ExperimentalWorldGen
    fun registerNoiseParameters(name: String, noiseParams: NoiseParameters): NoiseParameters {
        val id = ResourceLocation(addon, name)
        VanillaRegistries.NOISE[id] = noiseParams
        return noiseParams
    }
    
    @ExperimentalWorldGen
    fun registerNoiseParameters(name: String, firstOctave: Int, amplitudes: DoubleList) =
        registerNoiseParameters(name, NoiseParameters(firstOctave, amplitudes))
    
    @ExperimentalWorldGen
    fun registerNoiseParameters(name: String, firstOctave: Int, amplitudes: List<Double>) =
        registerNoiseParameters(name, NoiseParameters(firstOctave, DoubleArrayList(amplitudes)))
    
    @ExperimentalWorldGen
    fun registerNoiseParameters(name: String, firstOctave: Int, vararg amplitudes: Double) =
        registerNoiseParameters(name, NoiseParameters(firstOctave, DoubleArrayList(amplitudes)))
    
    @ExperimentalWorldGen
    fun registerNoiseGenerationSettings(name: String, settings: NoiseGeneratorSettings): NoiseGeneratorSettings {
        val id = ResourceLocation(addon, name)
        VanillaRegistries.NOISE_SETTINGS[id] = settings
        return settings
    }
    
}