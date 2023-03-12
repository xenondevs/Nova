package xyz.xenondevs.nova.world.generation.registry

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.biome.Biome
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.util.NMSUtils
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen

@ExperimentalWorldGen
object BiomeRegistry : WorldGenRegistry(NMSUtils.REGISTRY_ACCESS) {
    
    override val neededRegistries get() = setOf(Registries.BIOME)
    
    private val biomes = Object2ObjectOpenHashMap<NamespacedId, Biome>()
    
    fun registerBiome(addon: Addon, name: String, biome: Biome) {
        val id = NamespacedId(addon, name)
        require(id !in biomes) { "Duplicate biome $id" }
        biomes[id] = biome
    }
    
    override fun register() {
        loadFiles("biome", Biome.CODEC, biomes)
        registerAll(Registries.BIOME, biomes)
    }
}