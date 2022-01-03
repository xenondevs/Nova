package xyz.xenondevs.nova.player.advancement.power

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.SOLAR_PANEL
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object SolarPanelAdvancement : Advancement(NamespacedKey(NOVA, "solar_panel")) {
    
    init {
        setParent(FurnaceGeneratorAdvancement.key)
        addObtainCriteria(SOLAR_PANEL)
        setDisplayLocalized {
            it.setIcon(SOLAR_PANEL.toIcon())
        }
    }
    
}