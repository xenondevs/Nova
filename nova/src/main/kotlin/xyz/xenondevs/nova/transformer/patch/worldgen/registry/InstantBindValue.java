package xyz.xenondevs.nova.transformer.patch.worldgen.registry;

import net.minecraft.core.Holder;

/**
 * Can be used to directly bind a {@link Holder Holders} value in Minecraft's registries. Please note that Nova's registries
 * do this by default, so this class is only needed for Minecraft's registries.
 *
 * @see MappedRegistryPatch
 */
public class InstantBindValue {
    
    public Object value;
    
    public InstantBindValue(Object value) {
        this.value = value;
    }
    
}
