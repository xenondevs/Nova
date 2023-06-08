package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.advancements.critereon.DistancePredicate
import net.minecraft.advancements.critereon.EntityEquipmentPredicate
import net.minecraft.advancements.critereon.EntityFlagsPredicate
import net.minecraft.advancements.critereon.EntityPredicate
import net.minecraft.advancements.critereon.EntitySubPredicate
import net.minecraft.advancements.critereon.EntityTypePredicate
import net.minecraft.advancements.critereon.LocationPredicate
import net.minecraft.advancements.critereon.MinMaxBounds
import net.minecraft.advancements.critereon.MobEffectsPredicate
import net.minecraft.advancements.critereon.NbtPredicate
import net.minecraft.advancements.critereon.SlimePredicate
import net.minecraft.core.Holder
import net.minecraft.nbt.CompoundTag
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.animal.CatVariant
import net.minecraft.world.entity.animal.Fox
import net.minecraft.world.entity.animal.FrogVariant
import net.minecraft.world.entity.animal.MushroomCow
import net.minecraft.world.entity.animal.Parrot
import net.minecraft.world.entity.animal.Rabbit
import net.minecraft.world.entity.animal.TropicalFish
import net.minecraft.world.entity.animal.axolotl.Axolotl
import net.minecraft.world.entity.animal.horse.Llama
import net.minecraft.world.entity.animal.horse.Variant
import net.minecraft.world.entity.decoration.PaintingVariant
import net.minecraft.world.entity.npc.VillagerType
import net.minecraft.world.entity.vehicle.Boat
import xyz.xenondevs.nmsutils.internal.util.nmsType
import org.bukkit.entity.EntityType as BukkitEntityType

class EntityPredicateBuilder : PredicateBuilder<EntityPredicate>() {
    
    private var entityType = EntityTypePredicate.ANY
    private var distance = DistancePredicate.ANY
    private var location = LocationPredicate.ANY
    private var steppingOnLocation = LocationPredicate.ANY
    private var effects = MobEffectsPredicate.ANY
    private var nbt = NbtPredicate.ANY
    private var flags = EntityFlagsPredicate.ANY
    private var equipment = EntityEquipmentPredicate.ANY
    private var subPredicate = EntitySubPredicate.ANY
    private var vehicle = EntityPredicate.ANY
    private var passenger = EntityPredicate.ANY
    private var targetedEntity = EntityPredicate.ANY
    private var team: String? = null
    
    fun entityType(tag: TagKey<EntityType<*>>) {
        this.entityType = EntityTypePredicate.of(tag)
    }
    
    fun entityType(type: EntityType<*>) {
        this.entityType = EntityTypePredicate.of(type)
    }
    
    fun entityType(type: BukkitEntityType) {
        this.entityType = EntityTypePredicate.of(type.nmsType)
    }
    
    fun distance(init: DistancePredicateBuilder.() -> Unit) {
        this.distance = DistancePredicateBuilder().apply(init).build()
    }
    
    fun location(init: LocationPredicateBuilder.() -> Unit) {
        this.location = LocationPredicateBuilder().apply(init).build()
    }
    
    fun steppingOn(init: LocationPredicateBuilder.() -> Unit) {
        this.steppingOnLocation = LocationPredicateBuilder().apply(init).build()
    }
    
    fun effects(init: MobEffectsPredicateBuilder.() -> Unit) {
        this.effects = MobEffectsPredicateBuilder().apply(init).build()
    }
    
    fun nbt(nbt: CompoundTag) {
        this.nbt = NbtPredicate(nbt)
    }
    
    fun nbt(init: CompoundTag.() -> Unit) {
        this.nbt = NbtPredicate(CompoundTag().apply(init))
    }
    
    fun flags(init: EntityFlagsPredicateBuilder.() -> Unit) {
        this.flags = EntityFlagsPredicateBuilder().apply(init).build()
    }
    
    fun equipment(init: EntityEquipmentPredicateBuilder.() -> Unit) {
        this.equipment = EntityEquipmentPredicateBuilder().apply(init).build()
    }
    
    fun fishingHook(init: FishingHookPredicateBuilder.() -> Unit) {
        this.subPredicate = FishingHookPredicateBuilder().apply(init).build()
    }
    
    fun lightningBolt(init: LightningBoltPredicateBuilder.() -> Unit) {
        this.subPredicate = LightningBoltPredicateBuilder().apply(init).build()
    }
    
    fun player(init: PlayerPredicateBuilder.() -> Unit) {
        this.subPredicate = PlayerPredicateBuilder().apply(init).build()
    }
    
    fun slime(size: MinMaxBounds.Ints) {
        this.subPredicate = SlimePredicate.sized(size)
    }
    
    fun slime(size: IntRange) {
        this.subPredicate = SlimePredicate.sized(MinMaxBounds.Ints.between(size.first, size.last))
    }
    
    fun slime(size: Int) {
        this.subPredicate = SlimePredicate.sized(MinMaxBounds.Ints.exactly(size))
    }
    
    fun cat(variant: CatVariant) {
        this.subPredicate = EntitySubPredicate.variant(variant)
    }
    
    fun frog(variant: FrogVariant) {
        this.subPredicate = EntitySubPredicate.variant(variant)
    }
    
    fun axolotl(variant: Axolotl.Variant) {
        this.subPredicate = EntitySubPredicate.Types.AXOLOTL.createPredicate(variant)
    }
    
    fun boat(variant: Boat.Type) {
        this.subPredicate = EntitySubPredicate.Types.BOAT.createPredicate(variant)
    }
    
    fun fox(variant: Fox.Type) {
        this.subPredicate = EntitySubPredicate.Types.FOX.createPredicate(variant)
    }
    
    fun mooshroom(variant: MushroomCow.MushroomType) {
        this.subPredicate = EntitySubPredicate.Types.MOOSHROOM.createPredicate(variant)
    }
    
    fun painting(variant: Holder<PaintingVariant>) {
        this.subPredicate = EntitySubPredicate.Types.PAINTING.createPredicate(variant)
    }
    
    fun rabbit(variant: Rabbit.Variant) {
        this.subPredicate = EntitySubPredicate.Types.RABBIT.createPredicate(variant)
    }
    
    fun horse(variant: Variant) {
        this.subPredicate = EntitySubPredicate.Types.HORSE.createPredicate(variant)
    }
    
    fun llama(variant: Llama.Variant) {
        this.subPredicate = EntitySubPredicate.Types.LLAMA.createPredicate(variant)
    }
    
    fun villager(type: VillagerType) {
        this.subPredicate = EntitySubPredicate.Types.VILLAGER.createPredicate(type)
    }
    
    fun parrot(variant: Parrot.Variant) {
        this.subPredicate = EntitySubPredicate.Types.PARROT.createPredicate(variant)
    }
    
    fun tropicalFish(pattern: TropicalFish.Pattern) {
        this.subPredicate = EntitySubPredicate.Types.TROPICAL_FISH.createPredicate(pattern)
    }
    
    fun targetedEntity(init: EntityPredicateBuilder.() -> Unit) {
        this.targetedEntity = EntityPredicateBuilder().apply(init).build()
    }
    
    fun team(team: String) {
        this.team = team
    }
    
    override fun build(): EntityPredicate {
        return EntityPredicate.Builder()
            .entityType(entityType)
            .distance(distance)
            .located(location)
            .steppingOn(steppingOnLocation)
            .effects(effects)
            .nbt(nbt)
            .flags(flags)
            .equipment(equipment)
            .subPredicate(subPredicate)
            .vehicle(vehicle)
            .passenger(passenger)
            .targetedEntity(targetedEntity)
            .team(team)
            .build()
    }
    
}