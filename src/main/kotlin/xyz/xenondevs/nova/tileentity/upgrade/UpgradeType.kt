package xyz.xenondevs.nova.tileentity.upgrade

import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry.EFFICIENCY_UPGRADE
import xyz.xenondevs.nova.material.NovaMaterialRegistry.ENERGY_UPGRADE
import xyz.xenondevs.nova.material.NovaMaterialRegistry.SPEED_UPGRADE

enum class UpgradeType(val material: NovaMaterial) {
    SPEED(SPEED_UPGRADE),
    EFFICIENCY(EFFICIENCY_UPGRADE),
    ENERGY(ENERGY_UPGRADE);
}