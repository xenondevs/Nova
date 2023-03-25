package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.material.TileEntityConstructor
import xyz.xenondevs.nova.material.builder.NovaBlockBuilder
import xyz.xenondevs.nova.material.builder.TileEntityNovaBlockBuilder

interface BlockRegistry : AddonGetter {
    
    fun tileEntity(name: String, tileEntity: TileEntityConstructor) =
        TileEntityNovaBlockBuilder(addon, name, tileEntity)
    
    fun block(name: String): NovaBlockBuilder =
        NovaBlockBuilder(addon, name)
}