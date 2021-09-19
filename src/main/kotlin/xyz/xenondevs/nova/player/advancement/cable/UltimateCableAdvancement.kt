package xyz.xenondevs.nova.player.advancement.cable

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.ULTIMATE_CABLE
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object UltimateCableAdvancement : Advancement(NamespacedKey(NOVA, "ultimate_cable")) {
    
    init {
        setParent(EliteCableAdvancement.key)
        addObtainCriteria(ULTIMATE_CABLE)
        setDisplayLocalized {
            it.setIcon(ULTIMATE_CABLE.toIcon())
        }
    }
    
}