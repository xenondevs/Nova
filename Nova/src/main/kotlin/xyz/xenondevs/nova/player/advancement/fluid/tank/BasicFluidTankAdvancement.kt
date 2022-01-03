package xyz.xenondevs.nova.player.advancement.fluid.tank

import net.roxeez.advancement.Advancement
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.BASIC_FLUID_TANK
import xyz.xenondevs.nova.player.advancement.RootAdvancement
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object BasicFluidTankAdvancement : Advancement(NOVA, "basic_fluid_tank") {
    
    init {
        setParent(RootAdvancement.key)
        addObtainCriteria(BASIC_FLUID_TANK)
        setDisplayLocalized {
            it.setIcon(BASIC_FLUID_TANK.toIcon())
        }
    }
    
}