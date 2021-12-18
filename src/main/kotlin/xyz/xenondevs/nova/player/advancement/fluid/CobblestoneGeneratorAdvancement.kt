package xyz.xenondevs.nova.player.advancement.fluid

import net.roxeez.advancement.Advancement
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.COBBLESTONE_GENERATOR
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.fluid.tank.BasicFluidTankAdvancement
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object CobblestoneGeneratorAdvancement : Advancement(NOVA, "cobblestone_generator") {
    
    init {
        setParent(PumpAdvancement.key)
        addObtainCriteria(COBBLESTONE_GENERATOR)
        setDisplayLocalized {
            it.setIcon(COBBLESTONE_GENERATOR.toIcon())
        }
    }
    
}