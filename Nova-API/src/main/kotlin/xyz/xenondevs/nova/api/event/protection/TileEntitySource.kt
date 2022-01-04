package xyz.xenondevs.nova.api.event.protection

import xyz.xenondevs.nova.api.TileEntity

class TileEntitySource(val tileEntity: TileEntity) : Source(tileEntity.owner)