package xyz.xenondevs.nmsutils.advancement.predicate

import xyz.xenondevs.nmsutils.adapter.NonNullAdapter
import net.minecraft.advancements.critereon.FishingHookPredicate as MojangFishingHookPredicate

class FishingHookPredicate(
    val inOpenWater: Boolean? = null
) : Predicate {
    
    companion object : NonNullAdapter<FishingHookPredicate, MojangFishingHookPredicate>(MojangFishingHookPredicate.ANY) {
        
        override fun convert(value: FishingHookPredicate): MojangFishingHookPredicate {
            return MojangFishingHookPredicate.inOpenWater(value.inOpenWater ?: false)
        }
        
    }
    
    class Builder {
        
        private var inOpenWater: Boolean? = null
        
        fun inOpenWater(inOpenWater: Boolean) {
            this.inOpenWater = inOpenWater
        }
        
        internal fun build(): FishingHookPredicate {
            return FishingHookPredicate(inOpenWater)
        }
        
    }
    
}