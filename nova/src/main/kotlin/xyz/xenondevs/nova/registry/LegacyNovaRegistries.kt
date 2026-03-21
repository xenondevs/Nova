package xyz.xenondevs.nova.registry

import net.minecraft.core.WritableRegistry
import net.minecraft.resources.Identifier
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjection
import xyz.xenondevs.nova.world.generation.wrapper.WrapperBlock

@OptIn(ExperimentalWorldGen::class)
internal object LegacyNovaRegistries {
    
    @JvmField
    val WRAPPER_BLOCK: WritableRegistry<WrapperBlock> = simpleRegistry("wrapper_block")
    
    // TODO: nova block registry needs to update wrapper block registry
//    @JvmField
//    val BLOCK: WritableRegistry<NovaBlock> = wrappingRegistry("block", WRAPPER_BLOCK, ::WrapperBlock)
    
    @JvmField
    val BIOME_INJECTION: WritableRegistry<BiomeInjection> = simpleRegistry("biome_injection")
    
    private fun <E : Any> simpleRegistry(name: String): WritableRegistry<E> {
        val id = Identifier.fromNamespaceAndPath("nova", name)
        return LegacyNovaRegistryAccess.addRegistry(id)
    }
    
    private fun <E : Any, W : Any> wrappingRegistry(
        name: String,
        wrapperRegistry: WritableRegistry<W>,
        toWrapper: (E) -> W
    ): WritableRegistry<E> {
        val id = Identifier.fromNamespaceAndPath("nova", name)
        return LegacyNovaRegistryAccess.addRegistry(id) { key, lifecycle ->
            WrappingRegistry(key, lifecycle, wrapperRegistry, toWrapper)
        }
    }
    
}