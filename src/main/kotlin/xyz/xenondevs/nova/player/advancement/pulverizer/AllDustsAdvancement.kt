package xyz.xenondevs.nova.player.advancement.pulverizer

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object AllDustsAdvancement : Advancement(NamespacedKey(NOVA, "all_dusts")) {
    
    init {
        setParent(DustAdvancement.key)
        
        NovaMaterial.values()
            .filter { it.name.endsWith("DUST") }
            .forEach { addObtainCriteria(it) }
        
        setDisplayLocalized {
            it.setIcon(NovaMaterial.DIAMOND_DUST.toIcon())
        }
    }
    
}