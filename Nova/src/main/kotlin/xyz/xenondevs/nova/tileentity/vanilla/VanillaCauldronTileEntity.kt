package xyz.xenondevs.nova.tileentity.vanilla

import xyz.xenondevs.nova.data.world.block.state.VanillaTileEntityState
import xyz.xenondevs.nova.tileentity.network.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.network.fluid.container.CauldronFluidContainer
import xyz.xenondevs.nova.tileentity.network.fluid.holder.FluidHolder
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.emptyEnumMap
import xyz.xenondevs.nova.util.enumMapOf

class VanillaCauldronTileEntity internal constructor(
    blockState: VanillaTileEntityState
) : NetworkedVanillaTileEntity(blockState) {
    
    private val container = CauldronFluidContainer(uuid, block)
    private val fluidHolder = FluidHolder(
        this,
        container to NetworkConnectionType.BUFFER,
    ) { CUBE_FACES.associateWithTo(emptyEnumMap()) { NetworkConnectionType.BUFFER } }
    
    override val holders: Map<NetworkType, EndPointDataHolder> =
        enumMapOf(NetworkType.FLUID to fluidHolder)
    
    init {
        handleBlockUpdate()
    }
    
    override fun handleBlockUpdate() {
        container.handleBlockUpdate()
    }
    
    override fun saveData() {
        super.saveData()
        fluidHolder.saveData()
    }
    
}