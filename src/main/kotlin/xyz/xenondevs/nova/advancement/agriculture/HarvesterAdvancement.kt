package xyz.xenondevs.nova.advancement.agriculture

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.setDisplayLocalized
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

object HarvesterAdvancement : Advancement(NamespacedKey(NOVA, "harvester")) {
    
    init {
        setParent(PlanterAdvancement.key)
        addObtainCriteria(NovaMaterial.HARVESTER)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.HARVESTER.toIcon())
        }
    }
    
}