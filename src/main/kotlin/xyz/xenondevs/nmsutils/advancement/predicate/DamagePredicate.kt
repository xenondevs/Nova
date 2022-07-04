package xyz.xenondevs.nmsutils.advancement.predicate

import xyz.xenondevs.nmsutils.adapter.NonNullAdapter
import xyz.xenondevs.nmsutils.adapter.impl.DoubleBoundsAdapter
import xyz.xenondevs.nmsutils.advancement.AdvancementDsl
import net.minecraft.advancements.critereon.DamagePredicate as MojangDamagePredicate

class DamagePredicate(
    val blocked: Boolean?,
    val dealt: ClosedRange<Double>?,
    val taken: ClosedRange<Double>?,
    val type: DamageSourcePredicate?,
    val source: EntityPredicate?
) : Predicate {
    
    companion object : NonNullAdapter<DamagePredicate, MojangDamagePredicate>(MojangDamagePredicate.ANY) {
        
        override fun convert(value: DamagePredicate): MojangDamagePredicate {
            return MojangDamagePredicate(
                DoubleBoundsAdapter.toNMS(value.dealt),
                DoubleBoundsAdapter.toNMS(value.taken),
                EntityPredicate.toNMS(value.source),
                value.blocked,
                DamageSourcePredicate.toNMS(value.type)
            )
        }
        
    }
    
    @AdvancementDsl
    class Builder {
        
        private var blocked: Boolean? = null
        private var dealt: ClosedRange<Double>? = null
        private var taken: ClosedRange<Double>? = null
        private var type: DamageSourcePredicate? = null
        private var source: EntityPredicate? = null
        
        fun blocked(blocked: Boolean) {
            this.blocked = blocked
        }
        
        fun dealt(dealt: ClosedRange<Double>) {
            this.dealt = dealt
        }
        
        fun taken(taken: ClosedRange<Double>) {
            this.taken = taken
        }
        
        fun type(init: DamageSourcePredicate.Builder.() -> Unit) {
            this.type = DamageSourcePredicate.Builder().apply(init).build()
        }
        
        fun source(source: EntityPredicate) {
            this.source = source
        }
        
        internal fun build(): DamagePredicate {
            return DamagePredicate(blocked, dealt, taken, type, source)
        }
        
    }
    
}