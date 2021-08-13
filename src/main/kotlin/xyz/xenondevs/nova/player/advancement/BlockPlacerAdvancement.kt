package xyz.xenondevs.nova.player.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial

object BlockPlacerAdvancement : Advancement(NamespacedKey(NOVA, "block_placer")) {
    
    init {
        setParent(RootAdvancement.key)
        addObtainCriteria(NovaMaterial.BLOCK_PLACER)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.BLOCK_PLACER.toIcon())
        }
    }
    
}