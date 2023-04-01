package xyz.xenondevs.nova.transformer.patch.worldgen.registry;

import xyz.xenondevs.nova.world.generation.registry.WorldGenRegistry;

/**
 * Used in {@link MappedRegistryPatch} and {@link WorldGenRegistry} to disinguish Nova registry entries from vanilla ones.
 */
public class InstantBindValue {
    
    public Object value;
    
    public InstantBindValue(Object value) {
        this.value = value;
    }
    
}
