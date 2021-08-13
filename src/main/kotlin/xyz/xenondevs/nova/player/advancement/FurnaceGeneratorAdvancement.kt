package xyz.xenondevs.nova.player.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial

object FurnaceGeneratorAdvancement : Advancement(NamespacedKey(NOVA, "furnace_generator")) {
    
    init {
        setParent(RootAdvancement.key)
        addObtainCriteria(NovaMaterial.FURNACE_GENERATOR)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.FURNACE_GENERATOR.toIcon())
        }
    }
    
}