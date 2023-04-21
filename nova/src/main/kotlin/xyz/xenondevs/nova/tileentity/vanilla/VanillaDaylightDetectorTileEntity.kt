package xyz.xenondevs.nova.tileentity.vanilla

import xyz.xenondevs.nova.data.world.block.state.VanillaTileEntityState

internal class VanillaDaylightDetectorTileEntity(blockState: VanillaTileEntityState) : VanillaTileEntity(blockState) {
    
    override val type = Type.DAYLIGHT_DETECTOR
    
    var power: Int by storedValue("power") { 0 }
    
    override fun handleInitialized() = Unit
    override fun handleRemoved(unload: Boolean) = Unit
}