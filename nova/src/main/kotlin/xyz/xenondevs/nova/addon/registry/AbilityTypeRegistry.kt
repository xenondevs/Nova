package xyz.xenondevs.nova.addon.registry

import org.bukkit.entity.Player
import xyz.xenondevs.nova.world.player.ability.Ability
import xyz.xenondevs.nova.world.player.ability.AbilityType
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.set

interface AbilityTypeRegistry : AddonGetter {
    
    fun <T : Ability> registerAbilityType(name: String, abilityCreator: (Player) -> T): AbilityType<T> {
        val id = ResourceLocation(addon, name)
        val abilityType = AbilityType(id, abilityCreator)
        
        NovaRegistries.ABILITY_TYPE[id] = abilityType
        return abilityType
    }
    
}