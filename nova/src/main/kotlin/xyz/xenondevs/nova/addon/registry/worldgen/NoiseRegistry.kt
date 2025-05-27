package xyz.xenondevs.nova.addon.registry.worldgen

import it.unimi.dsi.fastutil.doubles.DoubleList
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings
import net.minecraft.world.level.levelgen.synth.NormalNoise.NoiseParameters
import xyz.xenondevs.nova.addon.REGISTRIES_DEPRECATION
import xyz.xenondevs.nova.addon.registry.AddonGetter
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen

@Deprecated(REGISTRIES_DEPRECATION)
interface NoiseRegistry : AddonGetter {
    
    @Deprecated(REGISTRIES_DEPRECATION)
    @ExperimentalWorldGen
    fun registerNoiseParameters(name: String, noiseParams: NoiseParameters): NoiseParameters =
        addon.registerNoiseParameters(name, noiseParams)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    @ExperimentalWorldGen
    fun registerNoiseParameters(name: String, firstOctave: Int, amplitudes: DoubleList) =
        addon.registerNoiseParameters(name, firstOctave, amplitudes)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    @ExperimentalWorldGen
    fun registerNoiseParameters(name: String, firstOctave: Int, amplitudes: List<Double>) =
        addon.registerNoiseParameters(name, firstOctave, amplitudes)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    @ExperimentalWorldGen
    fun registerNoiseParameters(name: String, firstOctave: Int, vararg amplitudes: Double) =
        addon.registerNoiseParameters(name, firstOctave, *amplitudes)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    @ExperimentalWorldGen
    fun registerNoiseGenerationSettings(name: String, settings: NoiseGeneratorSettings): NoiseGeneratorSettings =
        addon.registerNoiseGenerationSettings(name, settings)
    
}