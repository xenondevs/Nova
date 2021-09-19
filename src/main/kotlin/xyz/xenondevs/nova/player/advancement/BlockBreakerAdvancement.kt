package xyz.xenondevs.nova.player.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.BLOCK_BREAKER

object BlockBreakerAdvancement : Advancement(NamespacedKey(NOVA, "block_breaker")) {
    
    init {
        setParent(BlockPlacerAdvancement.key)
        addObtainCriteria(BLOCK_BREAKER)
        setDisplayLocalized {
            it.setIcon(BLOCK_BREAKER.toIcon())
        }
    }
    
}