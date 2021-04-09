package xyz.xenondevs.nova.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial

class FurnaceGeneratorAdvancement : Advancement(KEY) {
    
    init {
        setParent(RootAdvancement.KEY)
        addObtainCriteria(NovaMaterial.FURNACE_GENERATOR)
        setDisplay {
            it.setTitle("Generating Energy")
            it.setDescription("Craft a Furnace Generator")
            it.setIcon(NovaMaterial.FURNACE_GENERATOR.toIcon())
        }
    }
    
    companion object {
        val KEY = NamespacedKey(NOVA, "furnace_generator")
    }
    
}