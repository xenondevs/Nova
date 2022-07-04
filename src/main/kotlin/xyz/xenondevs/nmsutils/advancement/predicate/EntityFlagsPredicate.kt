package xyz.xenondevs.nmsutils.advancement.predicate

import xyz.xenondevs.nmsutils.adapter.NonNullAdapter
import xyz.xenondevs.nmsutils.advancement.AdvancementDsl
import net.minecraft.advancements.critereon.EntityFlagsPredicate as MojangEntityFlagsPredicate

class EntityFlagsPredicate(
    val onFire: Boolean?,
    val crouching: Boolean?,
    val sprinting: Boolean?,
    val swimming: Boolean?,
    val baby: Boolean?
) : Predicate {
    
    companion object : NonNullAdapter<EntityFlagsPredicate, MojangEntityFlagsPredicate>(MojangEntityFlagsPredicate.ANY) {
        
        override fun convert(value: EntityFlagsPredicate): MojangEntityFlagsPredicate {
            return MojangEntityFlagsPredicate(
                value.onFire, value.crouching, value.sprinting, value.swimming, value.baby
            )
        }
        
    }
    
    @AdvancementDsl
    class Builder {
        
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
        
        internal fun build(): EntityFlagsPredicate {
            return EntityFlagsPredicate(onFire, crouching, sprinting, swimming, baby)
        }
        
    }
    
}