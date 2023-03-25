package xyz.xenondevs.nova.addon.registry

import net.minecraft.resources.ResourceLocation
import org.bukkit.entity.Player
import xyz.xenondevs.nova.player.ability.Ability
import xyz.xenondevs.nova.player.ability.AbilityType
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.set

interface AbilityTypeRegistry: AddonGetter {
    
    fun <T : Ability> abilityType(name: String, abilityCreator: (Player) -> T): AbilityType<T> {
        val id = ResourceLocation(addon.description.id, name)
        val abilityType = AbilityType(id, abilityCreator)
        
        NovaRegistries.ABILITY_TYPE[id] = abilityType
        return abilityType
    }

}