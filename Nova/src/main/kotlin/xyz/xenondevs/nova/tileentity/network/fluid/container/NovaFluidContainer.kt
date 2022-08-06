package xyz.xenondevs.nova.tileentity.network.fluid.container

import xyz.xenondevs.nova.data.config.Reloadable
import xyz.xenondevs.nova.data.config.ValueReloadable
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import java.util.*

class NovaFluidContainer(
    uuid: UUID,
    allowedTypes: Set<FluidType>,
    type: FluidType?,
    amount: Long,
    baseCapacity: ValueReloadable<Long>,
    private val upgradeHolder: UpgradeHolder? = null
) : FluidContainer(uuid, allowedTypes, type, amount, baseCapacity.value), Reloadable {
    
    private val baseCapacity by baseCapacity
    
    init {
        if (upgradeHolder != null) reload()
    }
    
    override fun reload() {
        capacity = if (upgradeHolder != null)
            (baseCapacity * upgradeHolder.getValue(UpgradeType.FLUID)).toLong()
        else baseCapacity
    }
    
}