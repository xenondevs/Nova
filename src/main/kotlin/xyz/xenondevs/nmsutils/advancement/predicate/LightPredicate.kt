package xyz.xenondevs.nmsutils.advancement.predicate

import xyz.xenondevs.nmsutils.adapter.NonNullAdapter
import xyz.xenondevs.nmsutils.adapter.impl.IntBoundsAdapter
import net.minecraft.advancements.critereon.LightPredicate as LightPredicateAdapter

class LightPredicate(val level: IntRange?) : Predicate {
    
    companion object : NonNullAdapter<LightPredicate, LightPredicateAdapter>(LightPredicateAdapter.ANY) {
        
        override fun convert(value: LightPredicate): LightPredicateAdapter {
            return LightPredicateAdapter.Builder()
                .setComposite(IntBoundsAdapter.toNMS(value.level))
                .build()
        }
        
    }
    
}