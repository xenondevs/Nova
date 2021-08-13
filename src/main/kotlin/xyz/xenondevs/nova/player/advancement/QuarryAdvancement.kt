package xyz.xenondevs.nova.player.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial

object QuarryAdvancement : Advancement(NamespacedKey(NOVA, "quarry")) {
    
    init {
        setParent(BlockBreakerAdvancement.key)
        addObtainCriteria(NovaMaterial.QUARRY)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.QUARRY.toIcon())
        }
    }
    
}