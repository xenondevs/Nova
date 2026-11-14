package xyz.xenondevs.nova.compiler

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.compiler.plugin.devkit.DevKitComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

class NovaCompilerPluginRegistrar : DevKitComponentRegistrar {
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
