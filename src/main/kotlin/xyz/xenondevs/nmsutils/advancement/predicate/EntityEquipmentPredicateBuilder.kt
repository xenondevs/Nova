package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.advancements.critereon.EntityEquipmentPredicate
import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.advancements.critereon.EntityEquipmentPredicate as MojangEntityEquipmentPredicate

class EntityEquipmentPredicateBuilder : PredicateBuilder<EntityEquipmentPredicate>() {
    
    private var head = ItemPredicate.ANY
    private var chest = ItemPredicate.ANY
    private var legs = ItemPredicate.ANY
    private var feet = ItemPredicate.ANY
    private var mainHand = ItemPredicate.ANY
    private var offHand = ItemPredicate.ANY
    
    fun head(init: ItemPredicateBuilder.() -> Unit) {
        head = ItemPredicateBuilder().apply(init).build()
    }
    
    fun chest(init: ItemPredicateBuilder.() -> Unit) {
        chest = ItemPredicateBuilder().apply(init).build()
    }
    
    fun legs(init: ItemPredicateBuilder.() -> Unit) {
        legs = ItemPredicateBuilder().apply(init).build()
    }
    
    fun feet(init: ItemPredicateBuilder.() -> Unit) {
        feet = ItemPredicateBuilder().apply(init).build()
    }
    
    fun mainHand(init: ItemPredicateBuilder.() -> Unit) {
        mainHand = ItemPredicateBuilder().apply(init).build()
    }
    
    fun offHand(init: ItemPredicateBuilder.() -> Unit) {
        offHand = ItemPredicateBuilder().apply(init).build()
    }
    
    override fun build(): MojangEntityEquipmentPredicate {
        return MojangEntityEquipmentPredicate(head, chest, legs, feet, mainHand, offHand)
    }
    
}