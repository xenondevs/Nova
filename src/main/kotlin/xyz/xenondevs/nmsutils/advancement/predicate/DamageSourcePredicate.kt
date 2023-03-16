package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.advancements.critereon.TagPredicate
import net.minecraft.resources.ResourceKey
import net.minecraft.world.damagesource.DamageType
import xyz.xenondevs.nmsutils.adapter.NonNullAdapter
import xyz.xenondevs.nmsutils.advancement.AdvancementDsl
import xyz.xenondevs.nmsutils.internal.util.TagKey
import net.minecraft.advancements.critereon.DamageSourcePredicate as MojangDamageSourcePredicate

class DamageSourcePredicate(
    val tags: List<TagPredicate<DamageType>>?,
    val directEntity: EntityPredicate?,
    val sourceEntity: EntityPredicate?
) : Predicate {
    
    companion object : NonNullAdapter<DamageSourcePredicate, MojangDamageSourcePredicate>(MojangDamageSourcePredicate.ANY) {
        
        override fun convert(value: DamageSourcePredicate): MojangDamageSourcePredicate {
            return MojangDamageSourcePredicate(
                value.tags,
                EntityPredicate.toNMS(value.directEntity),
                EntityPredicate.toNMS(value.sourceEntity)
            )
        }
        
    }
    
    @AdvancementDsl
    class Builder {
        
        private var tags = ArrayList<TagPredicate<DamageType>>()
        private var directEntity: EntityPredicate? = null
        private var sourceEntity: EntityPredicate? = null
        
        fun type(key: ResourceKey<DamageType>, value: Boolean) {
            tags.add(TagPredicate(TagKey(key), value))
        }
        
        internal fun build(): DamageSourcePredicate {
            return DamageSourcePredicate(
                tags,
                directEntity,
                sourceEntity
            )
        }
        
    }
    
}