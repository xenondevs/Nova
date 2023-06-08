package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.advancements.critereon.EntityFlagsPredicate
import net.minecraft.advancements.critereon.EntityFlagsPredicate as MojangEntityFlagsPredicate

class EntityFlagsPredicateBuilder : PredicateBuilder<EntityFlagsPredicate>() {
    
    private var onFire: Boolean? = null
    private var crouching: Boolean? = null
    private var sprinting: Boolean? = null
    private var swimming: Boolean? = null
    private var baby: Boolean? = null
    
    fun onFire(onFire: Boolean) {
        this.onFire = onFire
    }
    
    fun crouching(crouching: Boolean) {
        this.crouching = crouching
    }
    
    fun sprinting(sprinting: Boolean) {
        this.sprinting = sprinting
    }
    
    fun swimming(swimming: Boolean) {
        this.swimming = swimming
    }
    
    fun baby(baby: Boolean) {
        this.baby = baby
    }
    
    override fun build(): MojangEntityFlagsPredicate {
        return MojangEntityFlagsPredicate(onFire, crouching, sprinting, swimming, baby)
    }
    
}