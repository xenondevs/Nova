package xyz.xenondevs.nova.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial

object VacuumChestAdvancement : Advancement(NamespacedKey(NOVA, "vacuum_chest")) {
    
    init {
        setParent(ItemFilterAdvancement.key)
        addObtainCriteria(NovaMaterial.VACUUM_CHEST)
        setDisplay {
            it.setTitle("This seems safe")
            it.setDescription("Craft a Vacuum Chest")
            it.setIcon(NovaMaterial.VACUUM_CHEST.toIcon())
        }
    }
    
}