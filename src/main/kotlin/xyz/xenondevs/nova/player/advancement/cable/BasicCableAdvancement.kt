package xyz.xenondevs.nova.player.advancement.cable

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.BASIC_CABLE
import xyz.xenondevs.nova.player.advancement.RootAdvancement
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object BasicCableAdvancement : Advancement(NamespacedKey(NOVA, "basic_cable")) {
    
    init {
        setParent(RootAdvancement.key)
        addObtainCriteria(BASIC_CABLE)
        setDisplayLocalized {
            it.setIcon(BASIC_CABLE.toIcon())
        }
    }
    
}