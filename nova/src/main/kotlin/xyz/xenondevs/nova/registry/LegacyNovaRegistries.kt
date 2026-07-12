package xyz.xenondevs.nova.registry

import net.minecraft.core.WritableRegistry
import net.minecraft.resources.Identifier
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjection

@OptIn(ExperimentalWorldGen::class)
internal object LegacyNovaRegistries {
    
    @JvmField
    val BIOME_INJECTION: WritableRegistry<BiomeInjection> = simpleRegistry("biome_injection")
    
    private fun <E : Any> simpleRegistry(name: String): WritableRegistry<E> {
        val id = Identifier.fromNamespaceAndPath("nova", name)
        return LegacyNovaRegistryAccess.addRegistry(id)
    }
    
}