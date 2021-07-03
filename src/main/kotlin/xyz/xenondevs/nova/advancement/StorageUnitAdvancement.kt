package xyz.xenondevs.nova.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial

object StorageUnitAdvancement : Advancement(NamespacedKey(NOVA, "storage_unit")) {
    
    init {
        setParent(VacuumChestAdvancement.key)
        addObtainCriteria(NovaMaterial.STORAGE_UNIT)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.STORAGE_UNIT.toIcon())
        }
    }
    
}