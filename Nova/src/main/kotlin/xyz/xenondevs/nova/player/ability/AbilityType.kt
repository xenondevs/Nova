package xyz.xenondevs.nova.player.ability

import org.bukkit.entity.Player
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.util.addNamespace

object AbilityTypeRegistry {
    
    private val abilityTypes = HashMap<String, AbilityType<*>>()
    
    fun <T : Ability> registerAbility(addon: Addon, name: String, createAbility: (Player) -> T): AbilityType<T> {
        val id = NamespacedId(addon.description.id, name)
        val ability = AbilityType(id, createAbility)
        abilityTypes[id.toString()] = ability
        return ability
    }
    
    fun <T : AbilityType<*>> getAbilityType(addon: Addon, name: String): T? {
        val id = name.addNamespace(addon.description.id)
        return getAbilityType(id)
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : AbilityType<*>> getAbilityType(id: String): T? {
        return abilityTypes[id] as? T
    }
    
}

class AbilityType<T : Ability> internal constructor(val id: NamespacedId, val createAbility: (Player) -> T)