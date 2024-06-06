package xyz.xenondevs.nova.tileentity.vanilla

import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.tileentity.network.node.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.type.fluid.container.CauldronFluidContainer
import xyz.xenondevs.nova.tileentity.network.type.fluid.holder.DefaultFluidHolder
import xyz.xenondevs.nova.tileentity.network.type.fluid.holder.FluidHolder
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.world.BlockPos
import java.util.*

internal class VanillaCauldronTileEntity internal constructor(
    pos: BlockPos,
    data: Compound
) : NetworkedVanillaTileEntity(pos, data) {
    
    override val type = Type.CAULDRON
    
    private lateinit var container: CauldronFluidContainer
    private lateinit var fluidHolder: FluidHolder
    override lateinit var holders: Set<EndPointDataHolder>
    
    override fun handleEnable() {
        container = CauldronFluidContainer(UUID(0L, 0L), pos.block)
        DefaultFluidHolder.tryConvertLegacy(this)?.let { storeData("fluidHolder", it) } // legacy conversion
        fluidHolder = DefaultFluidHolder(
            storedValue("fluidHolder", ::Compound),
            mapOf(container to NetworkConnectionType.BUFFER),
            { CUBE_FACES.associateWithTo(enumMap()) { container } },
            { CUBE_FACES.associateWithTo(enumMap()) { NetworkConnectionType.BUFFER } }
        )
        holders = setOf(fluidHolder)
        
        handleBlockUpdate()
    }
    
    override fun handleBlockUpdate() {
        container.handleBlockUpdate()
    }
    
}