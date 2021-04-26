package xyz.xenondevs.nova.advancement.cable

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

object EliteCableAdvancement : Advancement(NamespacedKey(NOVA, "elite_cable")) {
    
    init {
        setParent(AdvancedCableAdvancement.key)
        addObtainCriteria(NovaMaterial.ELITE_CABLE)
        setDisplay {
            it.setTitle("Even More Cable")
            it.setDescription("Craft an Elite Cable")
            it.setIcon(NovaMaterial.ELITE_CABLE.toIcon())
        }
    }
    
}