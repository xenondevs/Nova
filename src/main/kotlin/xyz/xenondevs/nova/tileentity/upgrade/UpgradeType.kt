package xyz.xenondevs.nova.tileentity.upgrade

import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterial.*

enum class UpgradeType(val material: NovaMaterial) {
    SPEED(SPEED_UPGRADE),
    EFFICIENCY(EFFICIENCY_UPGRADE),
    ENERGY(ENERGY_UPGRADE);
}