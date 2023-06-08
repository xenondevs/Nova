package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.ChangeDimensionTrigger
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import org.bukkit.World
import xyz.xenondevs.nmsutils.internal.util.resourceKey

class ChangeDimensionTriggerBuilder : TriggerBuilder<ChangeDimensionTrigger.TriggerInstance>() {
    
    private var from: ResourceKey<Level>? = null
    private var to: ResourceKey<Level>? = null
    
    fun from(world: ResourceKey<Level>) {
        from = world
    }
    
    fun from(world: World) {
        from = world.resourceKey
    }
    
    fun from(level: Level) {
        from = level.dimension()
    }
    
    fun to(world: ResourceKey<Level>) {
        to = world
    }
    
    fun to(world: World) {
        to = world.resourceKey
    }
    
    fun to(level: Level) {
        to = level.dimension()
    }
    
    override fun build(): ChangeDimensionTrigger.TriggerInstance {
        return ChangeDimensionTrigger.TriggerInstance(player, from, to)
    }
    
}