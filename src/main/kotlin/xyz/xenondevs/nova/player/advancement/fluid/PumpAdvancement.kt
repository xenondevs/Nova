package xyz.xenondevs.nova.player.advancement.fluid

import net.roxeez.advancement.Advancement
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.PUMP
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.fluid.tank.BasicFluidTankAdvancement
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object PumpAdvancement : Advancement(NOVA, "pump") {
    
    init {
        setParent(BasicFluidTankAdvancement.key)
        addObtainCriteria(PUMP)
        setDisplayLocalized {
            it.setIcon(PUMP.toIcon())
        }
    }
    
}