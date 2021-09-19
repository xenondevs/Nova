package xyz.xenondevs.nova.player.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.FURNACE_GENERATOR

object FurnaceGeneratorAdvancement : Advancement(NamespacedKey(NOVA, "furnace_generator")) {
    
    init {
        setParent(RootAdvancement.key)
        addObtainCriteria(FURNACE_GENERATOR)
        setDisplayLocalized {
            it.setIcon(FURNACE_GENERATOR.toIcon())
        }
    }
    
}