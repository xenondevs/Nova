package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.advancements.critereon.FishingHookPredicate

class FishingHookPredicateBuilder : PredicateBuilder<FishingHookPredicate>() {
    
    private var inOpenWater: Boolean? = null
    
    fun inOpenWater(inOpenWater: Boolean) {
        this.inOpenWater = inOpenWater
    }
    
    override fun build(): FishingHookPredicate {
        require(inOpenWater != null) { "inOpenWater is not set" }
        return FishingHookPredicate.inOpenWater(inOpenWater!!)
    }
    
}