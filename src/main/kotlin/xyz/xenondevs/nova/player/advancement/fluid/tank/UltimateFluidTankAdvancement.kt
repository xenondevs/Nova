package xyz.xenondevs.nova.player.advancement.fluid.tank

import net.roxeez.advancement.Advancement
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.ELITE_FLUID_TANK
import xyz.xenondevs.nova.material.NovaMaterialRegistry.ULTIMATE_FLUID_TANK
import xyz.xenondevs.nova.player.advancement.RootAdvancement
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object UltimateFluidTankAdvancement: Advancement(NOVA, "ultimate_fluid_tank") {
    
    init {
        setParent(EliteFluidTankAdvancement.key)
        addObtainCriteria(ULTIMATE_FLUID_TANK)
        setDisplayLocalized {
            it.setIcon(ULTIMATE_FLUID_TANK.toIcon())
        }
    }
    
}