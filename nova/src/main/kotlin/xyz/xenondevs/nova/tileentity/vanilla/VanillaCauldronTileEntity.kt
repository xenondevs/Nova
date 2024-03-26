package xyz.xenondevs.nova.tileentity.vanilla

import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.tileentity.network.DefaultNetworkTypes
import xyz.xenondevs.nova.tileentity.network.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.network.fluid.container.CauldronFluidContainer
import xyz.xenondevs.nova.tileentity.network.fluid.holder.NovaFluidHolder
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.world.BlockPos

internal class VanillaCauldronTileEntity internal constructor(
    pos: BlockPos,
    data: Compound
) : NetworkedVanillaTileEntity(pos, data) {
    
    override val type = Type.CAULDRON
    
    private val container = CauldronFluidContainer(uuid, pos.block)
    private val fluidHolder = NovaFluidHolder(
        this,
        container to NetworkConnectionType.BUFFER,
    ) { CUBE_FACES.associateWithTo(enumMap()) { NetworkConnectionType.BUFFER } }
    
    override val holders: Map<NetworkType, EndPointDataHolder> =
        hashMapOf(DefaultNetworkTypes.FLUID to fluidHolder)
    
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