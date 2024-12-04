package xyz.xenondevs.nova.addon.registry.worldgen

import net.minecraft.core.registries.Registries
import net.minecraft.world.level.levelgen.carver.CarverConfiguration
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver
import net.minecraft.world.level.levelgen.carver.WorldCarver
import xyz.xenondevs.nova.addon.registry.AddonGetter
import xyz.xenondevs.nova.patch.impl.registry.set
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen

interface CarverRegistry : AddonGetter {
    
    @ExperimentalWorldGen
    fun <CC : CarverConfiguration> registerCarver(name: String, carver: WorldCarver<CC>): WorldCarver<CC> {
        val id = ResourceLocation(addon, name)
        Registries.CARVER[id] = carver
        return carver
    }
    
    @ExperimentalWorldGen
    fun <CC : CarverConfiguration> registerConfiguredCarver(name: String, configuredCarver: ConfiguredWorldCarver<CC>): ConfiguredWorldCarver<CC> {
        val id = ResourceLocation(addon, name)
        Registries.CONFIGURED_CARVER[id] = configuredCarver
        return configuredCarver
    }
    
}