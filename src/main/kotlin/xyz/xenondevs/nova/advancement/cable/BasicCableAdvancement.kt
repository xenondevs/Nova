package xyz.xenondevs.nova.advancement.cable

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.RootAdvancement
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

class BasicCableAdvancement : Advancement(KEY) {
    
    init {
        setParent(RootAdvancement.KEY)
        addObtainCriteria(NovaMaterial.BASIC_CABLE)
        setDisplay {
            it.setTitle("Basic Networking")
            it.setDescription("Craft a Basic Cable")
            it.setIcon(NovaMaterial.BASIC_CABLE.toIcon())
        }
    }
    
    companion object {
        val KEY = NamespacedKey(NOVA, "basic_cable")
    }
    
}