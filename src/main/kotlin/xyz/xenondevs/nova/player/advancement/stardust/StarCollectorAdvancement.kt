package xyz.xenondevs.nova.player.advancement.stardust

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object StarCollectorAdvancement : Advancement(NamespacedKey(NOVA, "star_collector")) {
    
    init {
        setParent(StarShardsAdvancement.key)
        addObtainCriteria(NovaMaterialRegistry.STAR_COLLECTOR)
        setDisplayLocalized {
            it.setIcon(NovaMaterialRegistry.STAR_COLLECTOR.toIcon())
        }
    }
    
}