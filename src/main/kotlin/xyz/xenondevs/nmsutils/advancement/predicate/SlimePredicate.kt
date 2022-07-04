package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.advancements.critereon.EntitySubPredicate
import xyz.xenondevs.nmsutils.adapter.NonNullAdapter
import xyz.xenondevs.nmsutils.adapter.impl.IntBoundsAdapter
import xyz.xenondevs.nmsutils.advancement.AdvancementDsl
import net.minecraft.advancements.critereon.SlimePredicate as MojangSlimePredicate

class SlimePredicate(
    val size: IntRange?
) : Predicate {
    
    companion object : NonNullAdapter<SlimePredicate, EntitySubPredicate>(MojangSlimePredicate.ANY) {
        
        override fun convert(value: SlimePredicate): EntitySubPredicate {
            return MojangSlimePredicate.sized(IntBoundsAdapter.toNMS(value.size))
        }
        
    }
    
    @AdvancementDsl
    class Builder {
        
        private var size: IntRange? = null
        
        fun size(size: IntRange) {
            this.size = size
        }
        
        internal fun build(): SlimePredicate {
            return SlimePredicate(size)
        }
        
    }
    
}