package xyz.xenondevs.nova.player.advancement.cable

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.ADVANCED_CABLE
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object AdvancedCableAdvancement : Advancement(NamespacedKey(NOVA, "advanced_cable")) {
    
    init {
        setParent(BasicCableAdvancement.key)
        addObtainCriteria(ADVANCED_CABLE)
        setDisplayLocalized {
            it.setIcon(ADVANCED_CABLE.toIcon())
        }
    }
    
}