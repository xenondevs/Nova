package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.advancements.critereon.DamagePredicate
import net.minecraft.advancements.critereon.DamageSourcePredicate
import net.minecraft.advancements.critereon.EntityPredicate
import net.minecraft.advancements.critereon.MinMaxBounds

class DamagePredicateBuilder : PredicateBuilder<DamagePredicate>() {
    
    private var blocked: Boolean? = null
    private var dealt = MinMaxBounds.Doubles.ANY
    private var taken = MinMaxBounds.Doubles.ANY
    private var type = DamageSourcePredicate.ANY
    private var source = EntityPredicate.ANY
    
    fun blocked(blocked: Boolean) {
        this.blocked = blocked
    }
    
    fun dealt(dealt: MinMaxBounds.Doubles) {
        this.dealt = dealt
    }
    
    fun dealt(dealt: ClosedRange<Double>) {
        this.dealt = MinMaxBounds.Doubles.between(dealt.start, dealt.endInclusive)
    }
    
    fun dealt(dealt: Double) {
        this.dealt = MinMaxBounds.Doubles.exactly(dealt)
    }
    
    fun taken(taken: MinMaxBounds.Doubles) {
        this.taken = taken
    }
    
    fun taken(taken: ClosedRange<Double>) {
        this.taken = MinMaxBounds.Doubles.between(taken.start, taken.endInclusive)
    }
    
    fun taken(taken: Double) {
        this.taken = MinMaxBounds.Doubles.exactly(taken)
    }
    
    fun type(init: DamageSourcePredicateBuilder.() -> Unit) {
        this.type = DamageSourcePredicateBuilder().apply(init).build()
    }
    
    fun source(source: EntityPredicate) {
        this.source = source
    }
    
    override fun build(): DamagePredicate {
        return DamagePredicate(dealt, taken, source, blocked, type)
    }
    
}