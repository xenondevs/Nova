package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.core.Registry
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.util.resourceLocation
import net.minecraft.advancements.critereon.BrewedPotionTrigger as MojangBrewedPotionTrigger

class BrewedPotionTrigger(
    val player: EntityPredicate?,
    val id: String?
) : Trigger {
    
    companion object : Adapter<BrewedPotionTrigger, MojangBrewedPotionTrigger.TriggerInstance> {
        
        override fun toNMS(value: BrewedPotionTrigger): MojangBrewedPotionTrigger.TriggerInstance {
            return MojangBrewedPotionTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                value.id?.resourceLocation?.let(Registry.POTION::get)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<BrewedPotionTrigger>() {
        
        private var id: String? = null
        
        fun id(id: String) {
            this.id = id
        }
        
        override fun build(): BrewedPotionTrigger {
            return BrewedPotionTrigger(player, id)
        }
        
    }
    
}

/**
 * A singleton object containing the default potion ids.
 * Not an enum because additional ids can technically be registered.
 */
object PotionId {
    const val EMPTY = "minecraft:empty"
    const val WATER = "minecraft:water"
    const val MUNDANE = "minecraft:mundane"
    const val THICK = "minecraft:thick"
    const val AWKWARD = "minecraft:awkward"
    const val NIGHT_VISION = "minecraft:night_vision"
    const val LONG_NIGHT_VISION = "minecraft:long_night_vision"
    const val LEAPING = "minecraft:leaping"
    const val LONG_LEAPING = "minecraft:long_leaping"
    const val STRONG_LEAPING = "minecraft:strong_leaping"
    const val FIRE_RESISTANCE = "minecraft:fire_resistance"
    const val LONG_FIRE_RESISTANCE = "minecraft:long_fire_resistance"
    const val SWIFTNESS = "minecraft:swiftness"
    const val LONG_SWIFTNESS = "minecraft:long_swiftness"
    const val STRONG_SWIFTNESS = "minecraft:strong_swiftness"
    const val SLOWNESS = "minecraft:slowness"
    const val LONG_SLOWNESS = "minecraft:long_slowness"
    const val STRONG_SLOWNESS = "minecraft:strong_slowness"
    const val TURTLE_MASTER = "minecraft:turtle_master"
    const val LONG_TURTLE_MASTER = "minecraft:long_turtle_master"
    const val STRONG_TURTLE_MASTER = "minecraft:strong_turtle_master"
    const val WATER_BREATHING = "minecraft:water_breathing"
    const val LONG_WATER_BREATHING = "minecraft:long_water_breathing"
    const val HEALING = "minecraft:healing"
    const val STRONG_HEALING = "minecraft:strong_healing"
    const val HARMING = "minecraft:harming"
    const val STRONG_HARMING = "minecraft:strong_harming"
    const val POISON = "minecraft:poison"
    const val LONG_POISON = "minecraft:long_poison"
    const val STRONG_POISON = "minecraft:strong_poison"
    const val REGENERATION = "minecraft:regeneration"
    const val LONG_REGENERATION = "minecraft:long_regeneration"
    const val STRONG_REGENERATION = "minecraft:strong_regeneration"
    const val STRENGTH = "minecraft:strength"
    const val LONG_STRENGTH = "minecraft:long_strength"
    const val STRONG_STRENGTH = "minecraft:strong_strength"
    const val WEAKNESS = "minecraft:weakness"
    const val LONG_WEAKNESS = "minecraft:long_weakness"
    const val LUCK = "minecraft:luck"
    const val SLOW_FALLING = "minecraft:slow_falling"
    const val LONG_SLOW_FALLING = "minecraft:long_slow_falling"
}