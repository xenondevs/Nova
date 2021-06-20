package xyz.xenondevs.nova.advancement.mob

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.RootAdvancement
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

object BreederAdvancement : Advancement(NamespacedKey(NOVA, "breeder")) {
    
    init {
        setParent(BottledMobAdvancement.key)
        addObtainCriteria(NovaMaterial.BREEDER)
        setDisplay {
            it.setTitle("Factory farming")
            it.setDescription("Craft a Breeder")
            it.setIcon(NovaMaterial.BREEDER.toIcon())
        }
    }
    
}