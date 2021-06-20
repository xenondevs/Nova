package xyz.xenondevs.nova.advancement.pulverizer

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.press.MechanicalPressAdvancement
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

object DustAdvancement : Advancement(NamespacedKey(NOVA, "dust")) {
    
    init {
        setParent(PulverizerAdvancement.key)
        
        val criteria = NovaMaterial.values()
            .filter { it.name.endsWith("DUST") }
            .map { addObtainCriteria(it) }
        
        addRequirements(*criteria.toTypedArray())
        
        setDisplay {
            it.setTitle("Dust")
            it.setDescription("Pulverize something to get dust")
            it.setIcon(NovaMaterial.COPPER_DUST.toIcon())
        }
    }
    
}