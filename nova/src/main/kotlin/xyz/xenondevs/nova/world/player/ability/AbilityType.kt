package xyz.xenondevs.nova.world.player.ability

import kotlinx.serialization.Serializable
import org.bukkit.entity.Player
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.serialization.kotlinx.AbilityTypeSerializer

@Serializable(with = AbilityTypeSerializer::class)
class AbilityType<T : Ability> internal constructor(
    override val entry: RegistryEntry.Nova<AbilityType<T>>,
    val createAbility: (Player) -> T
) : NovaRegistryElement<AbilityType<T>> {
    override fun toString(): String = key.toString()
}