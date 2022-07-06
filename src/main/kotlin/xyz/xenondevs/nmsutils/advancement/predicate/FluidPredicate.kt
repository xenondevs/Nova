package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.core.Registry
import net.minecraft.tags.TagKey
import org.bukkit.Fluid
import org.bukkit.Tag
import xyz.xenondevs.nmsutils.adapter.NonNullAdapter
import xyz.xenondevs.nmsutils.advancement.AdvancementDsl
import xyz.xenondevs.nmsutils.internal.util.resourceLocation
import xyz.xenondevs.nmsutils.internal.util.tagKey
import net.minecraft.advancements.critereon.FluidPredicate as MojangFluidPredicate

class FluidPredicate(
    val tag: Tag<Fluid>?,
    val fluid: Fluid?,
    val properties: StatePropertiesPredicate?
) : Predicate {
    
    companion object : NonNullAdapter<FluidPredicate, MojangFluidPredicate>(MojangFluidPredicate.ANY) {
        
        @Suppress("UNCHECKED_CAST")
        override fun convert(value: FluidPredicate): MojangFluidPredicate {
            return MojangFluidPredicate(
                value.tag?.tagKey as TagKey<net.minecraft.world.level.material.Fluid>?,
                value.fluid?.let { Registry.FLUID.get(it.key.resourceLocation) },
                StatePropertiesPredicate.toNMS(value.properties)
            )
        }
        
    }
    
    @AdvancementDsl
    class Builder {
        
        private var tag: Tag<Fluid>? = null
        private var fluid: Fluid? = null
        private var properties: StatePropertiesPredicate? = null
        
        fun tag(tag: Tag<Fluid>) {
            this.tag = tag
        }
        
        fun fluid(fluid: Fluid) {
            this.fluid = fluid
        }
        
        fun properties(init: StatePropertiesPredicate.Builder.() -> Unit) {
            this.properties = StatePropertiesPredicate.Builder().apply(init).build()
        }
        
        internal fun build(): FluidPredicate {
            return FluidPredicate(tag, fluid, properties)
        }
        
        
    }
    
}