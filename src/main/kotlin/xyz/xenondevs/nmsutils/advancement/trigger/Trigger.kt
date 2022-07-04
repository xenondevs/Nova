package xyz.xenondevs.nmsutils.advancement.trigger

import xyz.xenondevs.nmsutils.advancement.AdvancementDsl
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate

sealed interface Trigger {
    
    @AdvancementDsl
    abstract class Builder<T : Trigger> {
        
        protected var player: EntityPredicate? = null
        
        fun player(init: EntityPredicate.Builder.() -> Unit) {
            player = EntityPredicate.Builder().apply(init).build()
        }
        
        internal abstract fun build(): T
        
    }
    
    abstract class PlayerBuilder<T : Trigger>(
        private val constructor: (EntityPredicate?) -> T
    ) : Builder<T>() {
        
        final override fun build(): T {
            return constructor(player)
        }
        
    }
    
}