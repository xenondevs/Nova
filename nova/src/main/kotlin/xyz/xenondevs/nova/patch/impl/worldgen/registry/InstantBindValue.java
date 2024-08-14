package xyz.xenondevs.nova.patch.impl.worldgen.registry;

import net.minecraft.core.Holder;
import org.jetbrains.annotations.ApiStatus;

/**
 * Can be used to directly bind a {@link Holder Holders} value in Minecraft's registries. Please note that Nova's registries
 * do this by default, so this class is only needed for Minecraft's registries.
 *
 * @see MappedRegistryPatch
 */
@ApiStatus.Internal
public class InstantBindValue {
    
    public Object value;
    
    public InstantBindValue(Object value) {
        this.value = value;
    }
    
}
