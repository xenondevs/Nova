package xyz.xenondevs.nova.player.advancement.fluid.tank

import net.roxeez.advancement.Advancement
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.ELITE_FLUID_TANK
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object EliteFluidTankAdvancement: Advancement(NOVA, "elite_fluid_tank") {
    
    init {
        setParent(AdvancedFluidTankAdvancement.key)
        addObtainCriteria(ELITE_FLUID_TANK)
        setDisplayLocalized {
            it.setIcon(ELITE_FLUID_TANK.toIcon())
        }
    }
    
}