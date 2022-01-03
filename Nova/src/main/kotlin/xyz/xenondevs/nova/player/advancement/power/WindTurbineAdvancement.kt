package xyz.xenondevs.nova.player.advancement.power

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object WindTurbineAdvancement : Advancement(NamespacedKey(NOVA, "wind_turbine")) {
    
    init {
        setParent(SolarPanelAdvancement.key)
        addObtainCriteria(NovaMaterialRegistry.WIND_TURBINE)
        setDisplayLocalized {
            it.setIcon(NovaMaterialRegistry.WIND_TURBINE.toIcon())
        }
    }
    
}