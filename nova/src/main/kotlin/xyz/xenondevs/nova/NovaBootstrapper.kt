@file:Suppress("UnstableApiUsage", "unused")

package xyz.xenondevs.nova

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.bootstrap.PluginProviderContext
import org.bukkit.plugin.java.JavaPlugin

internal class NovaBootstrapper : PluginBootstrap {
    
    override fun bootstrap(context: BootstrapContext) {
        // empty
    }
    
    override fun createPlugin(context: PluginProviderContext): JavaPlugin = Nova
    
}