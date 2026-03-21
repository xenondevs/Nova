package xyz.xenondevs.nova.registry

import xyz.xenondevs.nova.config.CONFIGS
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.TileEntityConstructor

internal class NovaTileEntityBlockBuilderImpl(
    entry: RegistryEntry.Nova<NovaBlock>,
    private val tileEntity: TileEntityConstructor
) : AbstractNovaBlockBuilder<NovaTileEntityBlock>(entry), NovaTileEntityBlockBuilder {
    
    private var tickrate: Int = 20
    
    override fun tickrate(tickrate: Int) {
        require(tickrate in 0..20) { "Sync TPS must be between 0 and 20" }
        this.tickrate = tickrate
    }
    
    override fun build() = NovaTileEntityBlock(
        entry,
        name.style(style),
        style,
        behaviors,
        tileEntity,
        tickrate,
        stateProperties,
        NovaItemBuilderImpl.blockItems[entry],
        CONFIGS[configId],
        blockStates
    )
    
}