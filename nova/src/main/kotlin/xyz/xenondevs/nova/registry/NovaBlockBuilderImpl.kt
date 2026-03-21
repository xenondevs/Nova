package xyz.xenondevs.nova.registry

import xyz.xenondevs.nova.config.CONFIGS
import xyz.xenondevs.nova.world.block.NovaBlock

internal open class NovaBlockBuilderImpl(
    entry: RegistryEntry.Nova<NovaBlock>
) : AbstractNovaBlockBuilder<NovaBlock>(entry) {
    
    override fun build() = NovaBlock(
        entry,
        name.style(style),
        style,
        behaviors,
        stateProperties,
        NovaItemBuilderImpl.blockItems[entry],
        CONFIGS[configId],
        blockStates
    )
    
}