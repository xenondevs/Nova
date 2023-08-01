package xyz.xenondevs.nova.api;

import org.bukkit.Bukkit;
import xyz.xenondevs.nova.api.block.BlockManager;
import xyz.xenondevs.nova.api.block.NovaBlockRegistry;
import xyz.xenondevs.nova.api.item.NovaItemRegistry;
import xyz.xenondevs.nova.api.material.NovaMaterialRegistry;
import xyz.xenondevs.nova.api.player.WailaManager;
import xyz.xenondevs.nova.api.protection.ProtectionIntegration;
import xyz.xenondevs.nova.api.tileentity.TileEntityManager;
import xyz.xenondevs.nova.loader.NovaJavaPlugin;

public interface Nova {
    
    /**
     * Gets the Nova instance.
     * @return The Nova instance.
     * @throws IllegalStateException If Nova is not installed on this server.
     */
    static Nova getNova() {
        NovaJavaPlugin novaPlugin = (NovaJavaPlugin) Bukkit.getPluginManager().getPlugin("Nova");
        if (novaPlugin == null)
            throw new IllegalStateException("Nova is not installed on this server.");
        
        return (Nova) novaPlugin.getNova();
    }
    
    /**
     * Gets the {@link TileEntityManager}.
     * @return The {@link TileEntityManager}.
     */
    TileEntityManager getTileEntityManager();
    
    /**
     * Gets the {@link BlockManager}.
     * @return The {@link BlockManager}.
     */
    BlockManager getBlockManager();
    
    /**
     * Gets the {@link NovaBlockRegistry}.
     * @return The {@link NovaBlockRegistry}.
     */
    NovaBlockRegistry getBlockRegistry();
    
    /**
     * Gets the {@link NovaItemRegistry}.
     * @return The {@link NovaItemRegistry}.
     */
    NovaItemRegistry getItemRegistry();
    
    /**
     * Gets the {@link WailaManager}.
     * @return The {@link WailaManager}.
     */
    WailaManager getWailaManager();
    
    /**
     * Gets the {@link NovaMaterialRegistry}.
     * @return The {@link NovaMaterialRegistry}.
     * @deprecated Use {@link NovaBlockRegistry} and {@link NovaItemRegistry} instead.
     */
    @Deprecated
    NovaMaterialRegistry getMaterialRegistry();
    
    /**
     * Registers a {@link ProtectionIntegration}.
     * @param integration The {@link ProtectionIntegration} to register.
     */
    void registerProtectionIntegration(ProtectionIntegration integration);
    
}
