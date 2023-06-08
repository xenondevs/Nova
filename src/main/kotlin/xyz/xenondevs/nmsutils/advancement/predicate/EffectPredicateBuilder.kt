package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.advancements.critereon.MinMaxBounds
import net.minecraft.advancements.critereon.MobEffectsPredicate
import net.minecraft.advancements.critereon.MobEffectsPredicate.MobEffectInstancePredicate
import net.minecraft.world.effect.MobEffect
import org.bukkit.potion.PotionEffectType
import xyz.xenondevs.nmsutils.internal.util.mobEffect

class MobEffectInstancePredicateBuilder : PredicateBuilder<MobEffectInstancePredicate>() {
    
    private var duration = MinMaxBounds.Ints.ANY
    private var amplifier = MinMaxBounds.Ints.ANY
    private var ambient: Boolean? = null
    private var particles: Boolean? = null
    
    fun duration(duration: MinMaxBounds.Ints) {
        this.duration = duration
    }
    
    fun duration(duration: IntRange) {
        this.duration = MinMaxBounds.Ints.between(duration.first, duration.last)
    }
    
    fun duration(duration: Int) {
        this.duration = MinMaxBounds.Ints.exactly(duration)
    }
    
    fun amplifier(amplifier: MinMaxBounds.Ints) {
        this.amplifier = amplifier
    }
    
    fun amplifier(amplifier: IntRange) {
        this.amplifier = MinMaxBounds.Ints.between(amplifier.first, amplifier.last)
    }
    
    fun amplifier(amplifier: Int) {
        this.amplifier = MinMaxBounds.Ints.exactly(amplifier)
    }
    
    fun ambient(ambient: Boolean) {
        this.ambient = ambient
    }
    
    fun particles(particles: Boolean) {
        this.particles = particles
    }
    
    override fun build(): MobEffectInstancePredicate {
        return MobEffectInstancePredicate(duration, amplifier, ambient, particles)
    }
    
}

class MobEffectsPredicateBuilder : PredicateBuilder<MobEffectsPredicate>() {
    
    private val effects = HashMap<MobEffect, MobEffectInstancePredicate>()
    
    fun effect(effect: PotionEffectType, instancePredicate: (MobEffectInstancePredicateBuilder.() -> Unit)) {
        effects[effect.mobEffect] = MobEffectInstancePredicateBuilder().apply(instancePredicate).build()
    }
    
    fun effect(effect: MobEffect, instancePredicate: (MobEffectInstancePredicateBuilder.() -> Unit)) {
        effects[effect] = MobEffectInstancePredicateBuilder().apply(instancePredicate).build()
    }
    
    fun effect(effect: PotionEffectType) {
        effects[effect.mobEffect] = MobEffectInstancePredicate()
    }
    
    fun effect(effect: MobEffect) {
        effects[effect] = MobEffectInstancePredicate()
    }
    
    override fun build(): MobEffectsPredicate {
        return MobEffectsPredicate(effects)
    }
    
}