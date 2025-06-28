package xyz.xenondevs.nova.addon.registry

import org.bukkit.entity.Player
import xyz.xenondevs.nova.addon.REGISTRIES_DEPRECATION
import xyz.xenondevs.nova.world.player.ability.Ability
import xyz.xenondevs.nova.world.player.ability.AbilityType

@Suppress("DEPRECATION")
@Deprecated(REGISTRIES_DEPRECATION)
interface AbilityTypeRegistry : AddonGetter {
    
    @Deprecated(REGISTRIES_DEPRECATION)
    fun <T : Ability> registerAbilityType(name: String, abilityCreator: (Player) -> T): AbilityType<T> =
        addon.registerAbilityType(name, abilityCreator)
    
}