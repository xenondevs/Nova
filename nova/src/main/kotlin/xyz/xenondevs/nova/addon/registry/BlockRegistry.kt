package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.addon.REGISTRIES_DEPRECATION
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.NovaBlockBuilder
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.NovaTileEntityBlockBuilder
import xyz.xenondevs.nova.world.block.TileEntityConstructor

@Deprecated(REGISTRIES_DEPRECATION)
interface BlockRegistry : AddonGetter {
    
    @Deprecated(REGISTRIES_DEPRECATION)
    fun tileEntity(name: String, constructor: TileEntityConstructor, tileEntity: NovaTileEntityBlockBuilder.() -> Unit): NovaTileEntityBlock =
        addon.tileEntity(name, constructor, tileEntity)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    fun block(name: String, block: NovaBlockBuilder.() -> Unit): NovaBlock =
        addon.block(name, block)
    
}