package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.advancements.critereon.EntitySubPredicate
import net.minecraft.advancements.critereon.LighthingBoltPredicate
import xyz.xenondevs.nmsutils.adapter.NonNullAdapter
import xyz.xenondevs.nmsutils.adapter.impl.IntBoundsAdapter
import xyz.xenondevs.nmsutils.advancement.AdvancementDsl
import xyz.xenondevs.nmsutils.internal.util.ReflectionRegistry

class LightningBoltPredicate(
    val blocksOnFire: IntRange?,
    val entityStruck: EntityPredicate?
) : Predicate {
    
    companion object : NonNullAdapter<LightningBoltPredicate, EntitySubPredicate>(LighthingBoltPredicate.ANY) {
        
        override fun convert(value: LightningBoltPredicate): LighthingBoltPredicate {
            return ReflectionRegistry.LIGHTNING_BOLT_PREDICATE_CONSTRUCTOR.newInstance(
                IntBoundsAdapter.toNMS(value.blocksOnFire),
                EntityPredicate.toNMS(value.entityStruck)
            )
        }
        
    }
    
    @AdvancementDsl
    class Builder {
        
        private var blocksOnFire: IntRange? = null
        private var entityStruck: EntityPredicate? = null
        
        fun blocksOnFire(range: IntRange) {
            blocksOnFire = range
        }
        
        fun entityStruck(init: EntityPredicate.Builder.() -> Unit) {
            entityStruck = EntityPredicate.Builder().apply(init).build()
        }
        
        internal fun build(): LightningBoltPredicate {
            return LightningBoltPredicate(blocksOnFire, entityStruck)
        }
        
    }
    
}