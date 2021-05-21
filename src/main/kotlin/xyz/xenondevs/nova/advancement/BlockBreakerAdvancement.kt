package xyz.xenondevs.nova.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.cable.BasicCableAdvancement
import xyz.xenondevs.nova.material.NovaMaterial

object BlockBreakerAdvancement : Advancement(NamespacedKey(NOVA, "block_breaker")) {
    
    init {
        setParent(BlockPlacerAdvancement.key)
        addObtainCriteria(NovaMaterial.BLOCK_BREAKER)
        setDisplay {
            it.setTitle("Automated Block Breaking")
            it.setDescription("Craft a Block Breaker")
            it.setIcon(NovaMaterial.BLOCK_BREAKER.toIcon())
        }
    }
    
}