package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.NovaBlockBuilder
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.NovaTileEntityBlockBuilder
import xyz.xenondevs.nova.world.block.TileEntityConstructor

interface BlockRegistry : AddonGetter {
    
    fun tileEntity(name: String, constructor: TileEntityConstructor, tileEntity: NovaTileEntityBlockBuilder.() -> Unit): NovaTileEntityBlock =
        NovaTileEntityBlockBuilder(addon, name, constructor).apply(tileEntity).register()
    
    fun block(name: String, block: NovaBlockBuilder.() -> Unit): NovaBlock =
        NovaBlockBuilder(addon, name).apply(block).register()
    
}