package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.advancements.critereon.StatePropertiesPredicate
import net.minecraft.util.StringRepresentable
import net.minecraft.world.level.block.state.properties.Property

class StatePropertiesPredicateBuilder : PredicateBuilder<StatePropertiesPredicate>() {
    
    private val builder = StatePropertiesPredicate.Builder.properties()
    
    fun property(property: Property<*>, value: String) {
        builder.hasProperty(property, value)
    }
    
    fun property(property: Property<Int>, value: Int) {
        builder.hasProperty(property, value)
    }
    
    fun property(property: Property<Boolean>, value: Boolean) {
        builder.hasProperty(property, value)
    }
    
    fun <T> property(property: Property<T>, value: T) where T : Comparable<T>, T : StringRepresentable {
        builder.hasProperty(property, value)
    }
    
    override fun build(): StatePropertiesPredicate = builder.build()
    
}