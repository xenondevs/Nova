package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.tags.TagKey
import org.bukkit.Tag
import org.bukkit.entity.EntityType
import xyz.xenondevs.nmsutils.adapter.NonNullAdapter
import xyz.xenondevs.nmsutils.util.nmsType
import xyz.xenondevs.nmsutils.util.tagKey
import net.minecraft.advancements.critereon.EntityTypePredicate as MojangEntityTypePredicate
import net.minecraft.world.entity.EntityType as MojangEntityType

sealed interface EntityTypePredicate : Predicate {
    
    class TagPredicate(val tag: Tag<EntityType>) : EntityTypePredicate
    class TypePredicate(val type: EntityType) : EntityTypePredicate
    
    companion object : NonNullAdapter<EntityTypePredicate, MojangEntityTypePredicate>(MojangEntityTypePredicate.ANY) {
        
        @Suppress("UNCHECKED_CAST")
        override fun convert(value: EntityTypePredicate): MojangEntityTypePredicate {
            return when (value) {
                is TypePredicate -> MojangEntityTypePredicate.of(value.type.nmsType)
                is TagPredicate -> MojangEntityTypePredicate.of(value.tag.tagKey as TagKey<MojangEntityType<*>>)
            }
        }
        
    }
    
}