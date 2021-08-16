package xyz.xenondevs.nova.player.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.SOLAR_PANEL

object SolarPanelAdvancement : Advancement(NamespacedKey(NOVA, "solar_panel")) {
    
    init {
        setParent(FurnaceGeneratorAdvancement.key)
        addObtainCriteria(SOLAR_PANEL)
        setDisplayLocalized {
            it.setIcon(SOLAR_PANEL.toIcon())
        }
    }
    
}