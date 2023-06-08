package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.EntityPredicate
import net.minecraft.advancements.critereon.MinMaxBounds
import net.minecraft.advancements.critereon.TargetBlockTrigger
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicateBuilder
import xyz.xenondevs.nmsutils.internal.util.asContextAwarePredicate

class TargetBlockTriggerBuilder : TriggerBuilder<TargetBlockTrigger.TriggerInstance>() {
    
    private var signalStrength = MinMaxBounds.Ints.ANY
    private var projectile = EntityPredicate.ANY
    
    fun signalStrength(signalStrength: MinMaxBounds.Ints) {
        this.signalStrength = signalStrength
    }
    
    fun signalStrength(signalStrength: IntRange) {
        this.signalStrength = MinMaxBounds.Ints.between(signalStrength.first, signalStrength.last)
    }
    
    fun signalStrength(signalStrength: Int) {
        this.signalStrength = MinMaxBounds.Ints.exactly(signalStrength)
    }
    
    fun projectile(init: EntityPredicateBuilder.() -> Unit) {
        this.projectile = EntityPredicateBuilder().apply(init).build()
    }
    
    override fun build() = TargetBlockTrigger.TriggerInstance(player, signalStrength, projectile.asContextAwarePredicate())
    
}