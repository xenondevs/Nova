package xyz.xenondevs.nova.tileentity.network.fluid.container

import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.data.config.Reloadable
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import java.util.*

fun NovaFluidContainer(
    uuid: UUID,
    allowedTypes: Set<FluidType>,
    type: FluidType?,
    amount: Long,
    baseCapacity: Provider<Long>,
    upgradeHolder: UpgradeHolder,
    upgradeType: UpgradeType<Double>
) = NovaFluidContainer(
    uuid, 
    allowedTypes,
    type, amount,
    baseCapacity,
    upgradeHolder as UpgradeHolder?,
    upgradeType as UpgradeType<Double>?
)

class NovaFluidContainer internal constructor(
    uuid: UUID,
    allowedTypes: Set<FluidType>,
    type: FluidType?,
    amount: Long,
    baseCapacity: Provider<Long>,
    private val upgradeHolder: UpgradeHolder?,
    private val upgradeType: UpgradeType<Double>?
) : FluidContainer(uuid, allowedTypes, type, amount, baseCapacity.value), Reloadable {
    
    private val baseCapacity by baseCapacity
    
    init {
        if (upgradeHolder != null) reload()
    }
    
    constructor(
        uuid: UUID,
        allowedTypes: Set<FluidType>,
        type: FluidType?,
        amount: Long,
        baseCapacity: Provider<Long>
    ) : this(
        uuid,
        allowedTypes,
        type,
        amount,
        baseCapacity,
        null, null
    )
    
    override fun reload() {
        capacity = if (upgradeHolder != null)
            (baseCapacity * upgradeHolder.getValue(upgradeType!!)).toLong()
        else baseCapacity
    }
    
}