package xyz.xenondevs.nova.player.ability

import net.minecraft.resources.ResourceLocation
import org.bukkit.entity.Player

class AbilityType<T : Ability> internal constructor(val id: ResourceLocation, val createAbility: (Player) -> T)