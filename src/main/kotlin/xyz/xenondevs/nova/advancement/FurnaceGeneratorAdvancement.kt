package xyz.xenondevs.nova.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial

object FurnaceGeneratorAdvancement : Advancement(NamespacedKey(NOVA, "furnace_generator")) {
    
    init {
        setParent(RootAdvancement.key)
        addObtainCriteria(NovaMaterial.FURNACE_GENERATOR)
        setDisplay {
            it.setTitle("Generating Energy")
            it.setDescription("Craft a Furnace Generator")
            it.setIcon(NovaMaterial.FURNACE_GENERATOR.toIcon())
        }
    }
    
}