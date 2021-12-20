package xyz.xenondevs.nova.tileentity.network.fluid.container

import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import java.util.*

class NovaFluidContainer(
    uuid: UUID,
    allowedTypes: Set<FluidType>,
    type: FluidType?,
    amount: Long,
    private val baseCapacity: Long,
    private val upgradeHolder: UpgradeHolder? = null
) : FluidContainer(uuid, allowedTypes, type, amount, baseCapacity) {
    
    init {
        upgradeHolder?.upgradeUpdateHandlers?.add(::handleUpgradeUpdates)
    }
    
    private fun handleUpgradeUpdates() {
        capacity = (baseCapacity * upgradeHolder!!.getFluidModifier()).toLong()
    }
    
}