package xyz.xenondevs.nova.player.ability

import org.bukkit.entity.Player
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId

object AbilityTypeRegistry {
    
    private val abilityTypes = HashMap<NamespacedId, AbilityType<*>>()
    
    fun <T : Ability> register(addon: Addon, name: String, createAbility: (Player) -> T): AbilityType<T> {
        val id = NamespacedId(addon.description.id, name)
        val ability = AbilityType(id, createAbility)
        abilityTypes[id] = ability
        return ability
    }
    
    fun <T : AbilityType<*>> of(addon: Addon, name: String): T? {
        val id = NamespacedId(name, addon.description.id)
        return of(id)
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : AbilityType<*>> of(id: NamespacedId): T? {
        return abilityTypes[id] as? T
    }
    
}

class AbilityType<T : Ability> internal constructor(val id: NamespacedId, val createAbility: (Player) -> T)