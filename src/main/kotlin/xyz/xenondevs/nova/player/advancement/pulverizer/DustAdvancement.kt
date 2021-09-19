package xyz.xenondevs.nova.player.advancement.pulverizer

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object DustAdvancement : Advancement(NamespacedKey(NOVA, "dust")) {
    
    init {
        setParent(PulverizerAdvancement.key)
        
        val criteria = NovaMaterialRegistry.values
            .filter { it.typeName.endsWith("DUST") }
            .map { addObtainCriteria(it) }
        
        addRequirements(*criteria.toTypedArray())
        
        setDisplayLocalized {
            it.setIcon(NovaMaterialRegistry.COPPER_DUST.toIcon())
        }
    }
    
}