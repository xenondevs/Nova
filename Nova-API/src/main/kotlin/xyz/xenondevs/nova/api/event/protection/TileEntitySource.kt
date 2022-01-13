package xyz.xenondevs.nova.api.event.protection

import xyz.xenondevs.nova.api.TileEntity

class TileEntitySource(val tileEntity: TileEntity) : Source(tileEntity.owner) {
    
    override fun toString(): String {
        return "TileEntitySource(tileEntity=$tileEntity, owner=$player)"
    }
    
}