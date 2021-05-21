package xyz.xenondevs.nova.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.cable.BasicCableAdvancement
import xyz.xenondevs.nova.material.NovaMaterial

object BlockPlacerAdvancement : Advancement(NamespacedKey(NOVA, "block_placer")) {
    
    init {
        setParent(RootAdvancement.key)
        addObtainCriteria(NovaMaterial.BLOCK_PLACER)
        setDisplay {
            it.setTitle("Automated Block Placement")
            it.setDescription("Craft a Block Placer")
            it.setIcon(NovaMaterial.BLOCK_PLACER.toIcon())
        }
    }
    
}