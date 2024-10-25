@file:Suppress("UnstableApiUsage")

package xyz.xenondevs.nova

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap

// required for addons to access Nova classpath during bootstrap phase
internal class NovaBootstrapper : PluginBootstrap {
    override fun bootstrap(context: BootstrapContext) {
        // empty
    }
}