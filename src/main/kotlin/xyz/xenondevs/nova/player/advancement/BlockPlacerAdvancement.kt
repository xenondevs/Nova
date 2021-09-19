package xyz.xenondevs.nova.player.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.BLOCK_PLACER

object BlockPlacerAdvancement : Advancement(NamespacedKey(NOVA, "block_placer")) {
    
    init {
        setParent(RootAdvancement.key)
        addObtainCriteria(BLOCK_PLACER)
        setDisplayLocalized {
            it.setIcon(BLOCK_PLACER.toIcon())
        }
    }
    
}