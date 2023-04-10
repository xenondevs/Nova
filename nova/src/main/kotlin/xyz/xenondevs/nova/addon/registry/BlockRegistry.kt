package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.world.block.NovaBlockBuilder
import xyz.xenondevs.nova.world.block.TileEntityConstructor
import xyz.xenondevs.nova.world.block.TileEntityNovaBlockBuilder

interface BlockRegistry : AddonGetter {
    
    fun tileEntity(name: String, tileEntity: TileEntityConstructor) =
        TileEntityNovaBlockBuilder(addon, name, tileEntity)
    
    fun block(name: String): NovaBlockBuilder =
        NovaBlockBuilder(addon, name)
}