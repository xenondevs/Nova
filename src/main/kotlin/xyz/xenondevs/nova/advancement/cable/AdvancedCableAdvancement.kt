package xyz.xenondevs.nova.advancement.cable

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

object AdvancedCableAdvancement : Advancement(NamespacedKey(NOVA, "advanced_cable")) {
    
    init {
        setParent(BasicCableAdvancement.key)
        addObtainCriteria(NovaMaterial.ADVANCED_CABLE)
        setDisplay {
            it.setTitle("Advanced Cable Technology")
            it.setDescription("Craft an Advanced Cable")
            it.setIcon(NovaMaterial.ADVANCED_CABLE.toIcon())
        }
    }
    
}