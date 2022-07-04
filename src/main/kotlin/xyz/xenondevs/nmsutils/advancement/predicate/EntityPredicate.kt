package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.advancements.critereon.EntitySubPredicate
import org.bukkit.Tag
import org.bukkit.entity.EntityType
import org.bukkit.potion.PotionEffect
import xyz.xenondevs.nmsutils.adapter.NonNullAdapter
import xyz.xenondevs.nmsutils.advancement.AdvancementDsl
import net.minecraft.advancements.critereon.EntityPredicate as MojangEntityPredicate

class EntityPredicate(
    val type: EntityTypePredicate?,
    val distance: DistancePredicate?,
    val location: LocationPredicate?,
    val steppingOnLocation: LocationPredicate?,
    val effects: List<EffectPredicate>?,
    val nbt: NbtPredicate?,
    val flags: EntityFlagsPredicate?,
    val equipment: EntityEquipmentPredicate?,
    val player: PlayerPredicate?,
    val fishingHook: FishingHookPredicate?,
    val lightningBolt: LightningBoltPredicate?,
    val slime: SlimePredicate?,
    val vehicle: EntityPredicate?,
    val passenger: EntityPredicate?,
    val targetedEntity: EntityPredicate?,
    val team: String?
) : Predicate {
    
    companion object : NonNullAdapter<EntityPredicate, MojangEntityPredicate>(MojangEntityPredicate.ANY) {
        
        override fun convert(value: EntityPredicate): MojangEntityPredicate {
            val builder = MojangEntityPredicate.Builder()
            
            builder.entityType(EntityTypePredicate.toNMS(value.type))
            builder.distance(DistancePredicate.toNMS(value.distance))
            builder.located(LocationPredicate.toNMS(value.location))
            builder.steppingOn(LocationPredicate.toNMS(value.steppingOnLocation))
            builder.effects(EffectPredicate.toNMS(value.effects))
            builder.nbt(NbtPredicate.toNMS(value.nbt))
            builder.flags(EntityFlagsPredicate.toNMS(value.flags))
            builder.equipment(EntityEquipmentPredicate.toNMS(value.equipment))
            builder.vehicle(toNMS(value.vehicle))
            builder.passenger(toNMS(value.passenger))
            builder.targetedEntity(toNMS(value.targetedEntity))
            builder.team(value.team)
            
            val subPredicate: EntitySubPredicate? = when {
                value.player != null -> PlayerPredicate.toNMS(value.player)
                value.lightningBolt != null -> LightningBoltPredicate.toNMS(value.lightningBolt)
                value.fishingHook != null -> FishingHookPredicate.toNMS(value.fishingHook)
                value.slime != null -> SlimePredicate.toNMS(value.slime)
                else -> null
            }
            
            if (subPredicate != null)
                builder.subPredicate(subPredicate)
            
            return builder.build()
        }
        
    }
    
    object EntityPredicateCompositeAdapter : NonNullAdapter<EntityPredicate, MojangEntityPredicate.Composite>(MojangEntityPredicate.Composite.ANY) {
        
        override fun convert(value: EntityPredicate): MojangEntityPredicate.Composite {
            return MojangEntityPredicate.Composite.wrap(EntityPredicate.toNMS(value))
        }
        
    }
    
    @AdvancementDsl
    class Builder {
        
        private val effects = ArrayList<EffectPredicate>()
        private var type: EntityTypePredicate? = null
        private var distance: DistancePredicate? = null
        private var location: LocationPredicate? = null
        private var steppingOnLocation: LocationPredicate? = null
        private var nbt: NbtPredicate? = null
        private var flags: EntityFlagsPredicate? = null
        private var equipment: EntityEquipmentPredicate? = null
        private var player: PlayerPredicate? = null
        private var fishingHook: FishingHookPredicate? = null
        private var lightningBolt: LightningBoltPredicate? = null
        private var slime: SlimePredicate? = null
        private var vehicle: EntityPredicate? = null
        private var passenger: EntityPredicate? = null
        private var targetedEntity: EntityPredicate? = null
        private var team: String? = null
        
        fun type(tag: Tag<EntityType>) {
            this.type = EntityTypePredicate.TagPredicate(tag)
        }
        
        fun type(type: EntityType) {
            this.type = EntityTypePredicate.TypePredicate(type)
        }
        
        fun distance(init: DistancePredicate.Builder.() -> Unit) {
            this.distance = DistancePredicate.Builder().apply(init).build()
        }
        
        fun location(init: LocationPredicate.Builder.() -> Unit) {
            this.location = LocationPredicate.Builder().apply(init).build()
        }
        
        fun steppingLocation(init: LocationPredicate.Builder.() -> Unit) {
            this.steppingOnLocation = LocationPredicate.Builder().apply(init).build()
        }
        
        fun effect(effect: PotionEffect) {
            effects += EffectPredicate.of(effect)
        }
        
        fun effect(init: EffectPredicate.Builder.() -> Unit) {
            effects += EffectPredicate.Builder().apply(init).build()
        }
        
        fun nbt(nbt: String) {
            this.nbt = NbtPredicate(nbt)
        }
        
        fun nbt(nbt: NbtPredicate) {
            this.nbt = nbt
        }
        
        fun flags(init: EntityFlagsPredicate.Builder.() -> Unit) {
            this.flags = EntityFlagsPredicate.Builder().apply(init).build()
        }
        
        fun equipment(init: EntityEquipmentPredicate.Builder.() -> Unit) {
            this.equipment = EntityEquipmentPredicate.Builder().apply(init).build()
        }
        
        fun player(init: PlayerPredicate.Builder.() -> Unit) {
            this.player = PlayerPredicate.Builder().apply(init).build()
        }
        
        fun fishingHook(init: FishingHookPredicate.Builder.() -> Unit) {
            this.fishingHook = FishingHookPredicate.Builder().apply(init).build()
        }
        
        fun lightningBolt(init: LightningBoltPredicate.Builder.() -> Unit) {
            this.lightningBolt = LightningBoltPredicate.Builder().apply(init).build()
        }
        
        fun slime(init: SlimePredicate.Builder.() -> Unit) {
            this.slime = SlimePredicate.Builder().apply(init).build()
        }
        
        fun vehicle(init: Builder.() -> Unit) {
            this.vehicle = Builder().apply(init).build()
        }
        
        fun passenger(init: Builder.() -> Unit) {
            this.passenger = Builder().apply(init).build()
        }
        
        fun targetedEntity(init: Builder.() -> Unit) {
            this.targetedEntity = Builder().apply(init).build()
        }
        
        fun team(team: String) {
            this.team = team
        }
        
        internal fun build(): EntityPredicate {
            return EntityPredicate(
                type,
                distance, location, steppingOnLocation,
                effects.takeUnless(List<*>::isEmpty),
                nbt, flags,
                equipment,
                player,
                fishingHook,
                lightningBolt,
                slime,
                vehicle,
                passenger,
                targetedEntity,
                team
            )
        }
        
    }
    
}