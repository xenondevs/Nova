@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.addon.registry.worldgen

import net.minecraft.world.level.levelgen.carver.CarverConfiguration
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver
import net.minecraft.world.level.levelgen.carver.WorldCarver
import xyz.xenondevs.nova.addon.REGISTRIES_DEPRECATION
import xyz.xenondevs.nova.addon.registry.AddonGetter
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen

@Deprecated(REGISTRIES_DEPRECATION)
interface CarverRegistry : AddonGetter {
    
    @Deprecated(REGISTRIES_DEPRECATION)
    @ExperimentalWorldGen
    fun <CC : CarverConfiguration> registerCarver(name: String, carver: WorldCarver<CC>): WorldCarver<CC> =
        addon.registerCarver(name, carver)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    @ExperimentalWorldGen
    fun <CC : CarverConfiguration> registerConfiguredCarver(name: String, configuredCarver: ConfiguredWorldCarver<CC>): ConfiguredWorldCarver<CC> =
        addon.registerConfiguredCarver(name, configuredCarver)
    
}