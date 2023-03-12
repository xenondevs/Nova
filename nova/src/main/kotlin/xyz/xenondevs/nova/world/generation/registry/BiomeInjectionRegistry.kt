package xyz.xenondevs.nova.world.generation.registry

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.core.registries.Registries
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.util.NMSUtils
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjection
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjectionBuilder
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjector

@ExperimentalWorldGen
object BiomeInjectionRegistry : WorldGenRegistry(NMSUtils.REGISTRY_ACCESS) {
    
    override val neededRegistries get() = setOf(Registries.BIOME)
    
    private val biomeInjections = Object2ObjectOpenHashMap<NamespacedId, BiomeInjection>()

    fun registerBiomeInjection(addon: Addon, name: String, injection: BiomeInjection): BiomeInjection {
        val id = NamespacedId(addon, name)
        require(id !in biomeInjections) { "Duplicate biome injection $id" }
        biomeInjections[id] = injection
        return injection
    }
    
    fun registerBiomeInjection(addon: Addon, name: String, builder: BiomeInjectionBuilder.() -> Unit) =
        registerBiomeInjection(addon, name, BiomeInjectionBuilder().apply(builder).build())
    
    override fun register() {
        loadFiles("inject/biome", BiomeInjection.CODEC, biomeInjections)
        BiomeInjector.loadInjections(biomeInjections.values)
    }
    
}