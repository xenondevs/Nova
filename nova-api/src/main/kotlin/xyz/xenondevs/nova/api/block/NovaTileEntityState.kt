package xyz.xenondevs.nova.api.block

import xyz.xenondevs.nova.api.tileentity.TileEntity

interface NovaTileEntityState : NovaBlockState {
    
    /**
     * The tile-entity represented by this [NovaTileEntityState].
     */
    val tileEntity: TileEntity
    
}