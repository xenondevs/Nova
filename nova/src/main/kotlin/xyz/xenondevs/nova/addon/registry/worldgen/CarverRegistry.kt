package xyz.xenondevs.nova.addon.registry.worldgen

import net.minecraft.world.level.levelgen.carver.CarverConfiguration
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver
import net.minecraft.world.level.levelgen.carver.WorldCarver
import xyz.xenondevs.nova.addon.registry.AddonHolder
import xyz.xenondevs.nova.registry.vanilla.VanillaRegistries
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen

interface CarverRegistry : AddonHolder {
    
    @ExperimentalWorldGen
    fun <CC : CarverConfiguration> registerCarver(name: String, carver: WorldCarver<CC>): WorldCarver<CC> {
        val id = ResourceLocation(addon, name)
        VanillaRegistries.CARVER[id] = carver
        return carver
    }
    
    @ExperimentalWorldGen
    fun <CC : CarverConfiguration> registerConfiguredCarver(name: String, configuredCarver: ConfiguredWorldCarver<CC>): ConfiguredWorldCarver<CC> {
        val id = ResourceLocation(addon, name)
        VanillaRegistries.CONFIGURED_CARVER[id] = configuredCarver
        return configuredCarver
    }
    
}