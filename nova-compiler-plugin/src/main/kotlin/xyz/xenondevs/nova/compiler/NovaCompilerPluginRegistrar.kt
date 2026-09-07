package xyz.xenondevs.nova.compiler

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

internal const val NOVA_COMPILER_PLUGIN_ID = "xyz.xenondevs.nova.compiler"

class NovaCompilerPluginRegistrar : CompilerPluginRegistrar() {
    
    override val pluginId: String = NOVA_COMPILER_PLUGIN_ID
    override val supportsK2: Boolean = true
    
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(NovaFirExtensionRegistrar())
    }
}

private class NovaFirExtensionRegistrar : FirExtensionRegistrar() {
    
    override fun ExtensionRegistrarContext.configurePlugin() {
        registerDiagnosticContainers(NovaDiagnostics)
        +::NovaCheckersExtension
    }
}
