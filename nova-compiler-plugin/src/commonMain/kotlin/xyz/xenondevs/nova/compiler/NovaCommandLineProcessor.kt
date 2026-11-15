package xyz.xenondevs.nova.compiler

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.devkit.DevKitCLP
import org.jetbrains.kotlin.config.CompilerConfiguration

class NovaCommandLineProcessor : DevKitCLP {
    
    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration,
    ) {
        error("Unexpected config option: '${option.optionName}'")
    }
    
    override val pluginOptions: Collection<CliOption>
        get() = emptyList()
    
}