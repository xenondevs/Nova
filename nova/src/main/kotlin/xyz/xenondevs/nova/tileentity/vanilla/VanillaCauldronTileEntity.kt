package xyz.xenondevs.nova.tileentity.vanilla

import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.tileentity.network.node.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.type.fluid.container.CauldronFluidContainer
import xyz.xenondevs.nova.tileentity.network.type.fluid.holder.DefaultFluidHolder
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.world.BlockPos
import java.util.*

internal class VanillaCauldronTileEntity internal constructor(
    pos: BlockPos,
    data: Compound
) : NetworkedVanillaTileEntity(pos, data) {
    
    override val type = Type.CAULDRON
    
    private val container = CauldronFluidContainer(UUID(0L, 0L), pos.block)
    private val fluidHolder = DefaultFluidHolder(
        storedValue("fluidHolder", ::Compound).get(), // TODO: legacy conversion
        mapOf(container to NetworkConnectionType.BUFFER),
        { CUBE_FACES.associateWithTo(enumMap()) { container }  },
        { CUBE_FACES.associateWithTo(enumMap()) { NetworkConnectionType.BUFFER } }
    )
    
    override val holders: Set<EndPointDataHolder> = setOf(fluidHolder)
    
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