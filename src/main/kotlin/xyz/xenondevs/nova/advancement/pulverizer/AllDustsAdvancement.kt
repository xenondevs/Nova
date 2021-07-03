package xyz.xenondevs.nova.advancement.pulverizer

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.setDisplayLocalized
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

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