package xyz.xenondevs.nova.advancement.pulverizer

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.RootAdvancement
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

object PulverizerAdvancement : Advancement(NamespacedKey(NOVA, "pulverizer")) {
    
    init {
        setParent(RootAdvancement.key)
        addObtainCriteria(NovaMaterial.PULVERIZER)
        setDisplay {
            it.setTitle("Basic Ore Duplication")
            it.setDescription("Craft a Pulverizer")
            it.setIcon(NovaMaterial.PULVERIZER.toIcon())
        }
    }
    
}