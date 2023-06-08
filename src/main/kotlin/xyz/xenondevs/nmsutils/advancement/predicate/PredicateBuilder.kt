package xyz.xenondevs.nmsutils.advancement.predicate

import xyz.xenondevs.nmsutils.advancement.AdvancementDsl

@AdvancementDsl
abstract class PredicateBuilder<T : Any> {
    
    internal abstract fun build(): T
    
}