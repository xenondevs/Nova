package xyz.xenondevs.nmsutils.advancement

import net.minecraft.resources.ResourceLocation
import org.bukkit.NamespacedKey
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.util.ReflectionRegistry.ADVANCEMENT_BUILDER_CONSTRUCTOR
import xyz.xenondevs.nmsutils.util.mapToArray
import xyz.xenondevs.nmsutils.util.resourceLocation

@DslMarker
internal annotation class AdvancementDsl

fun advancement(id: String, init: Advancement.Builder.() -> Unit): Advancement {
    return Advancement.Builder(id).apply(init).build()
}

class Advancement(
    val id: String,
    val parent: String?,
    val display: Display?,
    val criteria: List<Criterion>,
    val requirements: List<List<String>>?,
    val rewards: Rewards?
) {
    
    companion object : Adapter<Advancement, Pair<ResourceLocation, net.minecraft.advancements.Advancement.Builder>> {
        
        override fun toNMS(value: Advancement): Pair<ResourceLocation, net.minecraft.advancements.Advancement.Builder> {
            val builder = ADVANCEMENT_BUILDER_CONSTRUCTOR.newInstance()
            
            val parent = value.parent
            if (parent != null)
                builder.parent(parent.resourceLocation)
            
            val rewards = value.rewards
            if (rewards != null)
                builder.rewards(Rewards.toNMS(rewards))
            
            val requirements = value.requirements
            if (requirements != null)
                builder.requirements(requirements.mapToArray(List<String>::toTypedArray))
            
            val display = value.display
            if (display != null)
                builder.display(Display.toNMS(display))
            
            value.criteria.forEach { builder.addCriterion(it.name, Criterion.toNMS(it)) }
            
            return value.id.resourceLocation!! to builder
        }
        
    }
    
    @AdvancementDsl
    class Builder(private val id: String) {
        
        constructor(id: NamespacedKey) : this(id.toString())
        
        private var parent: String? = null
        private var display: Display? = null
        private var criteria: List<Criterion>? = null
        private var requirements: List<List<String>>? = null
        private var rewards: Rewards? = null
        
        fun parent(parent: String) {
            this.parent = parent
        }
        
        fun parent(parent: Advancement) {
            this.parent = parent.id
        }
        
        fun parent(parent: NamespacedKey) {
            this.parent = parent.toString()
        }
        
        fun display(init: Display.Builder.() -> Unit) {
            display = Display.Builder().apply(init).build()
        }
        
        fun rewards(init: Rewards.Builder.() -> Unit) {
            rewards = Rewards.Builder().apply(init).build()
        }
        
        fun criteria(init: CriteriaBuilder.() -> Unit) {
            criteria = CriteriaBuilder().apply(init).build()
        }
        
        fun requirements(init: RequirementsBuilder.() -> Unit) {
            requirements = RequirementsBuilder().apply(init).build()
        }
        
        internal fun build(): Advancement {
            checkNotNull(criteria) { "Advancement criteria are not set" }
            
            return Advancement(id, parent, display, criteria!!, requirements, rewards)
        }
        
    }
    
}
