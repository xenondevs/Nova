package xyz.xenondevs.nova.player.advancement.agriculture

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object HarvesterAdvancement : Advancement(NamespacedKey(NOVA, "harvester")) {
    
    init {
        setParent(FertilizerAdvancement.key)
        addObtainCriteria(NovaMaterial.HARVESTER)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.HARVESTER.toIcon())
        }
    }
    
}