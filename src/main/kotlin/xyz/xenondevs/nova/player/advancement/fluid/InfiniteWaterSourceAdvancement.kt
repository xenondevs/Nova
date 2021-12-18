package xyz.xenondevs.nova.player.advancement.fluid

import net.roxeez.advancement.Advancement
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.INFINITE_WATER_SOURCE
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.fluid.tank.UltimateFluidTankAdvancement
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object InfiniteWaterSourceAdvancement : Advancement(NOVA, "infinite_water_source"){
    
    init {
        setParent(UltimateFluidTankAdvancement.key)
        addObtainCriteria(INFINITE_WATER_SOURCE)
        setDisplayLocalized {
            it.setIcon(INFINITE_WATER_SOURCE.toIcon())
        }
    }
    
}