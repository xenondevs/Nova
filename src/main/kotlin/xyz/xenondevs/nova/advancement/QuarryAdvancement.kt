package xyz.xenondevs.nova.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.cable.BasicCableAdvancement
import xyz.xenondevs.nova.material.NovaMaterial

object QuarryAdvancement : Advancement(NamespacedKey(NOVA, "quarry")) {
    
    init {
        setParent(BlockBreakerAdvancement.key)
        addObtainCriteria(NovaMaterial.QUARRY)
        setDisplay {
            it.setTitle("Automated Mining")
            it.setDescription("Craft a Quarry")
            it.setIcon(NovaMaterial.QUARRY.toIcon())
        }
    }
    
}