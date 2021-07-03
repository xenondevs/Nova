package xyz.xenondevs.nova.advancement.mob

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.RootAdvancement
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.setDisplayLocalized
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

object BottledMobAdvancement : Advancement(NamespacedKey(NOVA, "bottled_mob")) {
    
    init {
        setParent(RootAdvancement.key)
        addObtainCriteria(NovaMaterial.BOTTLED_MOB)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.BOTTLED_MOB.toIcon())
        }
    }
    
}