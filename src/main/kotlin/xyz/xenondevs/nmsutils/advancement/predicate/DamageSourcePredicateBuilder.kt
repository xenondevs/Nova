package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.advancements.critereon.DamageSourcePredicate
import net.minecraft.advancements.critereon.EntityPredicate
import net.minecraft.advancements.critereon.TagPredicate
import net.minecraft.resources.ResourceKey
import net.minecraft.world.damagesource.DamageType
import xyz.xenondevs.nmsutils.internal.util.TagKey

class DamageSourcePredicateBuilder : PredicateBuilder<DamageSourcePredicate>() {
    
    private var tags = ArrayList<TagPredicate<DamageType>>()
    private var directEntity = EntityPredicate.ANY
    private var sourceEntity = EntityPredicate.ANY
    
    fun type(key: ResourceKey<DamageType>, value: Boolean) {
        tags.add(TagPredicate(TagKey(key), value))
    }
    
    override fun build(): DamageSourcePredicate {
        return DamageSourcePredicate(
            tags,
            directEntity,
            sourceEntity
        )
    }
    
}