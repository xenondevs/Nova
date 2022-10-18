package xyz.xenondevs.nova.world.generation.registry

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjection
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjector

object BiomeInjectionRegistry : WorldGenRegistry() {
    
    override val neededRegistries = setOf(Registry.BIOME_REGISTRY)
    
    private val biomeInjections = Object2ObjectOpenHashMap<NamespacedId, BiomeInjection>()
    
    override fun loadFiles() {
        biomeInjections += loadFiles("inject/biome", BiomeInjection.CODEC)
        BiomeInjector.loadInjections(biomeInjections.values)
    }
    
    override fun register(registryAccess: RegistryAccess) = Unit
    
    fun registerBiomeInjection(addon: Addon, name: String, injection: BiomeInjection) {
        val id = NamespacedId(addon, name)
        require(id !in biomeInjections) { "Duplicate biome injection $id" }
        biomeInjections[id] = injection
    }
    
}