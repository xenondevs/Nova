package xyz.xenondevs.nova.world.player.ability

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player

class AbilityType<T : Ability> internal constructor(val id: Key, val createAbility: (Player) -> T)