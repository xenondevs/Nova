package xyz.xenondevs.nova.advancement.farn

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.RootAdvancement
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.setDisplayLocalized
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

object PlanterAdvancement : Advancement(NamespacedKey(NOVA, "planter")) {
    
    init {
        setParent(RootAdvancement.key)
        addObtainCriteria(NovaMaterial.PLANTER)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.PLANTER.toIcon())
        }
    }
    
}