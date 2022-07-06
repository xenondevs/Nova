package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.advancements.critereon.MobEffectsPredicate
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import xyz.xenondevs.nmsutils.adapter.NonNullAdapter
import xyz.xenondevs.nmsutils.adapter.impl.IntBoundsAdapter
import xyz.xenondevs.nmsutils.advancement.AdvancementDsl
import xyz.xenondevs.nmsutils.internal.util.mobEffect

class EffectPredicate(
    val type: PotionEffectType,
    val duration: IntRange?,
    val amplifier: IntRange?,
    val ambient: Boolean?,
    val particles: Boolean?
) {
    
    companion object : NonNullAdapter<List<EffectPredicate>, MobEffectsPredicate>(MobEffectsPredicate.ANY) {
        
        override fun convert(value: List<EffectPredicate>): MobEffectsPredicate {
            return MobEffectsPredicate(
                value.associate {
                    it.type.mobEffect to MobEffectsPredicate.MobEffectInstancePredicate(
                        IntBoundsAdapter.toNMS(it.amplifier),
                        IntBoundsAdapter.toNMS(it.duration),
                        it.ambient,
                        it.particles
                    )
                }
            )
        }
        
        fun of(effect: PotionEffect): EffectPredicate {
            return EffectPredicate(
                effect.type,
                IntRange(effect.duration, effect.duration),
                IntRange(effect.amplifier, effect.amplifier),
                effect.isAmbient,
                effect.hasParticles()
            )
        }
        
    }
    
    @AdvancementDsl
    class Builder {
        
        private var type: PotionEffectType? = null
        private var duration: IntRange? = null
        private var amplifier: IntRange? = null
        private var ambient: Boolean? = null
        private var particles: Boolean? = null
        
        fun type(type: PotionEffectType) {
            this.type = type
        }
        
        fun duration(duration: IntRange) {
            this.duration = duration
        }
        
        fun amplifier(amplifier: IntRange) {
            this.amplifier = amplifier
        }
        
        fun ambient(ambient: Boolean) {
            this.ambient = ambient
        }
        
        fun particles(particles: Boolean) {
            this.particles = particles
        }
        
        internal fun build(): EffectPredicate {
            checkNotNull(type) { "type is not set" }
            return EffectPredicate(type!!, duration, amplifier, ambient, particles)
        }
        
    }
    
}