package xyz.xenondevs.nova.player.advancement.fluid.tank

import net.roxeez.advancement.Advancement
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.ADVANCED_FLUID_TANK
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object AdvancedFluidTankAdvancement : Advancement(NOVA, "advanced_fluid_tank") {
    
    init {
        setParent(BasicFluidTankAdvancement.key)
        addObtainCriteria(ADVANCED_FLUID_TANK)
        setDisplayLocalized {
            it.setIcon(ADVANCED_FLUID_TANK.toIcon())
        }
    }
    
}