package xyz.xenondevs.nova.world.generation.registry

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.world.level.levelgen.carver.CarverConfiguration
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver
import net.minecraft.world.level.levelgen.carver.WorldCarver
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId

object CarverRegistry : WorldGenRegistry() {
    
    override val neededRegistries = setOf(Registry.CARVER_REGISTRY, Registry.CONFIGURED_CARVER_REGISTRY)
    
    private val carvers = Object2ObjectOpenHashMap<NamespacedId, WorldCarver<*>>()
    private val configuredCarvers = Object2ObjectOpenHashMap<NamespacedId, ConfiguredWorldCarver<*>>()
    
    fun <CC : CarverConfiguration> registerCarver(addon: Addon, name: String, carver: WorldCarver<CC>) {
        val id = NamespacedId(addon, name)
        require(id !in carvers) { "Duplicate carver $id" }
        carvers[id] = carver
    }
    
    fun <CC : CarverConfiguration> registerConfiguredCarver(addon: Addon, name: String, configuredCarver: ConfiguredWorldCarver<CC>) {
        val id = NamespacedId(addon, name)
        require(id !in configuredCarvers) { "Duplicate configured carver $id" }
        this.configuredCarvers[id] = configuredCarver
    }
    
    override fun register(registryAccess: RegistryAccess) {
        registerAll(registryAccess, Registry.CARVER_REGISTRY, carvers)
        loadFiles("configured_carver", ConfiguredWorldCarver.CODEC, configuredCarvers)
        registerAll(registryAccess, Registry.CONFIGURED_CARVER_REGISTRY, configuredCarvers)
    }
}