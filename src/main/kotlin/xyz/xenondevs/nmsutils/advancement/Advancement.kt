package xyz.xenondevs.nmsutils.advancement

import net.minecraft.advancements.AdvancementRewards
import net.minecraft.advancements.DisplayInfo
import net.minecraft.resources.ResourceLocation
import net.minecraft.advancements.Advancement as MojangAdvancement

@DslMarker
internal annotation class AdvancementDsl

fun advancement(id: String, init: Advancement.Builder.() -> Unit): Advancement {
    return Advancement.Builder(id).apply(init).build()
}

fun advancement(id: ResourceLocation, init: Advancement.Builder.() -> Unit): Advancement {
    return Advancement.Builder(id).apply(init).build()
}

class Advancement internal constructor(
    val id: ResourceLocation,
    val parent: ResourceLocation?,
    val display: DisplayInfo?,
    val rewards: AdvancementRewards,
    val criteria: List<Criterion>,
    val requirements: Array<Array<String>>?
) {
    
    internal fun toNMS(): Pair<ResourceLocation, MojangAdvancement.Builder> {
        val builder = MojangAdvancement.Builder.recipeAdvancement()
            .parent(parent)
            .display(display)
            .rewards(rewards)
            .requirements(requirements)
        
        for (criterion in criteria)
            builder.addCriterion(criterion.name, criterion.trigger)
        
        
        return id to builder
    }
    
    
    @AdvancementDsl
    class Builder(val id: ResourceLocation) {
        
        private var parent: ResourceLocation? = null
        private var displayInfo: DisplayInfo? = null
        private var rewards = AdvancementRewards.EMPTY
        private var criteria = ArrayList<Criterion>()
        private var requirements: Array<Array<String>>? = null
        
        constructor(id: String) : this(ResourceLocation(id))
        
        fun parent(advancement: Advancement) {
            parent = advancement.id
        }
        
        fun parent(advancement: MojangAdvancement) {
            parent = advancement.id
        }
        
        fun parent(parent: ResourceLocation) {
            this.parent = parent
        }
        
        fun parent(parent: String) {
            this.parent = ResourceLocation(parent)
        }
        
        fun display(init: DisplayInfoBuilder.() -> Unit) {
            displayInfo = DisplayInfoBuilder().apply(init).build()
        }
        
        fun rewards(init: AdvancementRewardsBuilder.() -> Unit) {
            rewards = AdvancementRewardsBuilder().apply(init).build()
        }
        
        fun criteria(init: CriteriaBuilder.() -> Unit) {
            criteria.addAll(CriteriaBuilder().apply(init).build())
        }
        
        fun requirements(init: RequirementsBuilder.() -> Unit) {
            this.requirements = RequirementsBuilder().apply(init).build()
        }
        
        internal fun build(): Advancement =
            Advancement(id, parent, displayInfo, rewards, criteria, requirements)
        
    }
    
}