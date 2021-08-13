package xyz.xenondevs.nova.player.advancement.cable

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object AdvancedCableAdvancement : Advancement(NamespacedKey(NOVA, "advanced_cable")) {
    
    init {
        setParent(BasicCableAdvancement.key)
        addObtainCriteria(NovaMaterial.ADVANCED_CABLE)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.ADVANCED_CABLE.toIcon())
        }
    }
    
}