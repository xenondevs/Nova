package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.advancements.critereon.EntityPredicate
import net.minecraft.advancements.critereon.LighthingBoltPredicate
import net.minecraft.advancements.critereon.MinMaxBounds
import xyz.xenondevs.nmsutils.LightningBoltPredicate

class LightningBoltPredicateBuilder : PredicateBuilder<LighthingBoltPredicate>() {
    
    private var blocksOnFire = MinMaxBounds.Ints.ANY
    private var entityStruck = EntityPredicate.ANY
    
    fun blocksOnFire(range: MinMaxBounds.Ints) {
        blocksOnFire = range
    }
    
    fun blocksOnFire(range: IntRange) {
        blocksOnFire = MinMaxBounds.Ints.between(range.first, range.last)
    }
    
    fun blocksOnFire(blocksOnFire: Int) {
        this.blocksOnFire = MinMaxBounds.Ints.exactly(blocksOnFire)
    }
    
    fun entityStruck(init: EntityPredicateBuilder.() -> Unit) {
        entityStruck = EntityPredicateBuilder().apply(init).build()
    }
    
    override fun build() = LightningBoltPredicate(blocksOnFire, entityStruck)
    
}