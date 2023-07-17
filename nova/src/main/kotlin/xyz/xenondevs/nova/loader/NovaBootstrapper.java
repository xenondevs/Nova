package xyz.xenondevs.nova.loader;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.nova.Nova;

public class NovaBootstrapper implements PluginBootstrap {
    
    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
    }
    
    @Override
    public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext context) {
        return new Nova(context.getPluginSource().toFile());
    }
    
}
