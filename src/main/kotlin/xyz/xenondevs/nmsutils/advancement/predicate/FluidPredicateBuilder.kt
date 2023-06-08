package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.advancements.critereon.FluidPredicate
import net.minecraft.advancements.critereon.StatePropertiesPredicate
import net.minecraft.tags.TagKey
import net.minecraft.world.level.material.Fluid

class FluidPredicateBuilder : PredicateBuilder<FluidPredicate>() {
    
    private var tag: TagKey<Fluid>? = null
    private var fluid: Fluid? = null
    private var properties = StatePropertiesPredicate.ANY
    
    fun tag(tag: TagKey<Fluid>) {
        this.tag = tag
    }
    
    fun fluid(fluid: Fluid) {
        this.fluid = fluid
    }
    
    fun properties(init: StatePropertiesPredicateBuilder.() -> Unit) {
        this.properties = StatePropertiesPredicateBuilder().apply(init).build()
    }
    
    override fun build(): FluidPredicate {
        return FluidPredicate(tag, fluid, properties)
    }
    
}