package xyz.xenondevs.nmsutils.advancement.predicate

import xyz.xenondevs.nmsutils.adapter.NonNullAdapter
import xyz.xenondevs.nmsutils.advancement.AdvancementDsl
import net.minecraft.advancements.critereon.EntityEquipmentPredicate as MojangEntityEquipmentPredicate

class EntityEquipmentPredicate(
    val head: ItemPredicate?,
    val chest: ItemPredicate?,
    val legs: ItemPredicate?,
    val feet: ItemPredicate?,
    val mainHand: ItemPredicate?,
    val offHand: ItemPredicate?
) : Predicate {
    
    companion object : NonNullAdapter<EntityEquipmentPredicate, MojangEntityEquipmentPredicate>(MojangEntityEquipmentPredicate.ANY) {
        
        override fun convert(value: EntityEquipmentPredicate): MojangEntityEquipmentPredicate {
            return MojangEntityEquipmentPredicate(
                ItemPredicate.toNMS(value.head),
                ItemPredicate.toNMS(value.chest),
                ItemPredicate.toNMS(value.legs),
                ItemPredicate.toNMS(value.feet),
                ItemPredicate.toNMS(value.mainHand),
                ItemPredicate.toNMS(value.offHand),
            )
        }
        
    }
    
    @AdvancementDsl
    class Builder {
        
        private var head: ItemPredicate? = null
        private var chest: ItemPredicate? = null
        private var legs: ItemPredicate? = null
        private var feet: ItemPredicate? = null
        private var mainHand: ItemPredicate? = null
        private var offHand: ItemPredicate? = null
        
        fun head(init: ItemPredicate.Builder.() -> Unit) {
            head = ItemPredicate.Builder().apply(init).build()
        }
        
        fun chest(init: ItemPredicate.Builder.() -> Unit) {
            chest = ItemPredicate.Builder().apply(init).build()
        }
        
        fun legs(init: ItemPredicate.Builder.() -> Unit) {
            legs = ItemPredicate.Builder().apply(init).build()
        }
        
        fun feet(init: ItemPredicate.Builder.() -> Unit) {
            feet = ItemPredicate.Builder().apply(init).build()
        }
        
        fun mainHand(init: ItemPredicate.Builder.() -> Unit) {
            mainHand = ItemPredicate.Builder().apply(init).build()
        }
        
        fun offHand(init: ItemPredicate.Builder.() -> Unit) {
            offHand = ItemPredicate.Builder().apply(init).build()
        }
        
        internal fun build(): EntityEquipmentPredicate {
            return EntityEquipmentPredicate(head, chest, legs, feet, mainHand, offHand)
        }
        
    }
    
}