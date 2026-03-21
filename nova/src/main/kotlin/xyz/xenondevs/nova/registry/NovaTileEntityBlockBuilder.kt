package xyz.xenondevs.nova.registry

import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.tileentity.TileEntity

/**
 * A builder for [NovaTileEntityBlock].
 */
@RegistryElementBuilderDsl
sealed interface NovaTileEntityBlockBuilder : NovaBlockBuilder {
    
    /**
     * Configures the amount of times [TileEntity.handleTick] is called per second.
     * Accepts values from 0 to 20, with 0 disabling ticking.
     *
     * Defaults to 20.
     */
    fun tickrate(tickrate: Int)
    
}