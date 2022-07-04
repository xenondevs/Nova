package xyz.xenondevs.nmsutils.advancement.predicate

import xyz.xenondevs.nmsutils.adapter.NonNullAdapter
import xyz.xenondevs.nmsutils.advancement.AdvancementDsl
import xyz.xenondevs.nmsutils.util.ReflectionRegistry.EXACT_PROPERTY_MATCHER_CONSTRUCTOR
import xyz.xenondevs.nmsutils.util.ReflectionRegistry.RANGED_PROPERTY_MATCHER_CONSTRUCTOR
import xyz.xenondevs.nmsutils.util.ReflectionRegistry.STATE_PROPERTIES_PREDICATE_CONSTRUCTOR
import net.minecraft.advancements.critereon.StatePropertiesPredicate as MojangStatePropertiesPredicate

class StatePropertiesPredicate(
    val properties: List<StatePropertyPredicate>?
) : Predicate {
    
    companion object : NonNullAdapter<StatePropertiesPredicate, MojangStatePropertiesPredicate>(MojangStatePropertiesPredicate.ANY) {
        
        override fun convert(value: StatePropertiesPredicate): MojangStatePropertiesPredicate {
            val list = value.properties?.map {
                when (it) {
                    is StatePropertyPredicate.Exact -> EXACT_PROPERTY_MATCHER_CONSTRUCTOR.newInstance(it.key, it.value)
                    is StatePropertyPredicate.Ranged -> RANGED_PROPERTY_MATCHER_CONSTRUCTOR.newInstance(it.key, it.range.start, it.range.endInclusive)
                }
            } ?: emptyList()
            
            return STATE_PROPERTIES_PREDICATE_CONSTRUCTOR.newInstance(list) as MojangStatePropertiesPredicate
        }
        
    }
    
    @AdvancementDsl
    class Builder {
        
        private val properties = ArrayList<StatePropertyPredicate>()
        
        fun property(key: String, value: String) {
            properties += StatePropertyPredicate.Exact(key, value)
        }
        
        fun property(key: String, range: ClosedRange<*>) {
            properties += StatePropertyPredicate.Ranged(key, range)
        }
        
        internal fun build(): StatePropertiesPredicate {
            return StatePropertiesPredicate(properties)
        }
        
    }
    
}

sealed interface StatePropertyPredicate : Predicate {
    
    class Exact(
        val key: String,
        val value: String
    ) : StatePropertyPredicate
    
    class Ranged(
        val key: String,
        val range: ClosedRange<*>
    ) : StatePropertyPredicate
    
}
