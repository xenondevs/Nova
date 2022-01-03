package xyz.xenondevs.nova.player.advancement.cable

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.ELITE_CABLE
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object EliteCableAdvancement : Advancement(NamespacedKey(NOVA, "elite_cable")) {
    
    init {
        setParent(AdvancedCableAdvancement.key)
        addObtainCriteria(ELITE_CABLE)
        setDisplayLocalized {
            it.setIcon(ELITE_CABLE.toIcon())
        }
    }
    
}