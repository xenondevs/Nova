package xyz.xenondevs.nmsutils.advancement

import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.trigger.*
import net.minecraft.advancements.Criterion as MojangCriterion

class Criterion(
    val name: String,
    val trigger: Trigger
) {
    
    companion object : Adapter<Criterion, MojangCriterion> {
        
        override fun toNMS(value: Criterion): MojangCriterion {
            return MojangCriterion(TriggerRegistry.toNMS(value.trigger))
        }
        
    }
    
}

@AdvancementDsl
class CriteriaBuilder {
    
    private val criteria = ArrayList<Criterion>()
    
    fun criterion(name: String, trigger: Trigger): Criterion {
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun allayDropItemOnBlock(name: String, init: AllayDropItemOnBlockTrigger.Builder.() -> Unit): Criterion {
        val trigger = AllayDropItemOnBlockTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun avoidVibration(name: String, init: AvoidVibrationTrigger.Builder.() -> Unit): Criterion {
        val trigger = AvoidVibrationTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun beeNestDestroyed(name: String, init: BeeNestDestroyedTrigger.Builder.() -> Unit): Criterion {
        val trigger = BeeNestDestroyedTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun bredAnimals(name: String, init: BredAnimalsTrigger.Builder.() -> Unit): Criterion {
        val trigger = BredAnimalsTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun brewedPotion(name: String, init: BrewedPotionTrigger.Builder.() -> Unit): Criterion {
        val trigger = BrewedPotionTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun changedDimension(name: String, init: ChangedDimensionTrigger.Builder.() -> Unit): Criterion {
        val trigger = ChangedDimensionTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun channeledLightning(name: String, init: ChanneledLightningTrigger.Builder.() -> Unit): Criterion {
        val trigger = ChanneledLightningTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun constructBeacon(name: String, init: ConstructBeaconTrigger.Builder.() -> Unit): Criterion {
        val trigger = ConstructBeaconTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun consumeItem(name: String, init: ConsumeItemTrigger.Builder.() -> Unit): Criterion {
        val trigger = ConsumeItemTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun curedZombieVillager(name: String, init: CuredZombieVillagerTrigger.Builder.() -> Unit): Criterion {
        val trigger = CuredZombieVillagerTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun effectsChanged(name: String, init: EffectsChangedTrigger.Builder.() -> Unit): Criterion {
        val trigger = EffectsChangedTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun enchantedItem(name: String, init: EnchantedItemTrigger.Builder.() -> Unit): Criterion {
        val trigger = EnchantedItemTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun enterBlock(name: String, init: EnterBlockTrigger.Builder.() -> Unit): Criterion {
        val trigger = EnterBlockTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun entityHurtPlayer(name: String, init: EntityHurtPlayerTrigger.Builder.() -> Unit): Criterion {
        val trigger = EntityHurtPlayerTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun entityKilledPlayer(name: String, init: EntityKilledPlayerTrigger.Builder.() -> Unit): Criterion {
        val trigger = EntityKilledPlayerTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun fallFromHeight(name: String, init: FallFromHeightTrigger.Builder.() -> Unit): Criterion {
        val trigger = FallFromHeightTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun filledBucket(name: String, init: FilledBucketTrigger.Builder.() -> Unit): Criterion {
        val trigger = FilledBucketTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun fishingRodHooked(name: String, init: FishingRodHookedTrigger.Builder.() -> Unit): Criterion {
        val trigger = FishingRodHookedTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun heroOfTheVillage(name: String, init: HeroOfTheVillageTrigger.Builder.() -> Unit): Criterion {
        val trigger = HeroOfTheVillageTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun impossible(): Criterion {
        return Criterion("impossible", ImpossibleTrigger).apply(criteria::add)
    }
    
    fun inventoryChanged(name: String, init: InventoryChangedTrigger.Builder.() -> Unit): Criterion {
        val trigger = InventoryChangedTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun itemDurabilityChanged(name: String, init: ItemDurabilityChangedTrigger.Builder.() -> Unit): Criterion {
        val trigger = ItemDurabilityChangedTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun itemUsedOnBlock(name: String, init: ItemUsedOnBlockTrigger.Builder.() -> Unit): Criterion {
        val trigger = ItemUsedOnBlockTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun killedByCrossbow(name: String, init: KilledByCrossbowTrigger.Builder.() -> Unit): Criterion {
        val trigger = KilledByCrossbowTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun killMobNearSculkCatalyst(name: String, init: KillMobNearSculkCatalystTrigger.Builder.() -> Unit): Criterion {
        val trigger = KillMobNearSculkCatalystTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun levitation(name: String, init: LevitationTrigger.Builder.() -> Unit): Criterion {
        val trigger = LevitationTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun lightningStrike(name: String, init: LightningStrikeTrigger.Builder.() -> Unit): Criterion {
        val trigger = LightningStrikeTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun location(name: String, init: LocationTrigger.Builder.() -> Unit): Criterion {
        val trigger = LocationTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun netherTravel(name: String, init: NetherTravelTrigger.Builder.() -> Unit): Criterion {
        val trigger = NetherTravelTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun placedBlock(name: String, init: PlacedBlockTrigger.Builder.() -> Unit): Criterion {
        val trigger = PlacedBlockTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun playerGeneratesContainerLoot(name: String, init: PlayerGeneratesContainerLootTrigger.Builder.() -> Unit): Criterion {
        val trigger = PlayerGeneratesContainerLootTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun playerHurtEntity(name: String, init: PlayerHurtEntityTrigger.Builder.() -> Unit): Criterion {
        val trigger = PlayerHurtEntityTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun playerInteractedWithEntity(name: String, init: PlayerInteractedWithEntityTrigger.Builder.() -> Unit): Criterion {
        val trigger = PlayerInteractedWithEntityTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun playerKilledEntity(name: String, init: PlayerKilledEntityTrigger.Builder.() -> Unit): Criterion {
        val trigger = PlayerKilledEntityTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun recipeUnlocked(name: String, init: RecipeUnlockedTrigger.Builder.() -> Unit): Criterion {
        val trigger = RecipeUnlockedTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun rideEntityInLava(name: String, init: RideEntityInLavaTrigger.Builder.() -> Unit): Criterion {
        val trigger = RideEntityInLavaTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun shotCrossbow(name: String, init: ShotCrossbowTrigger.Builder.() -> Unit): Criterion {
        val trigger = ShotCrossbowTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun sleptInBed(name: String, init: SleptInBedTrigger.Builder.() -> Unit): Criterion {
        val trigger = SleptInBedTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun slideDownBlock(name: String, init: SlideDownBlockTrigger.Builder.() -> Unit): Criterion {
        val trigger = SlideDownBlockTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun startedRiding(name: String, init: StartedRidingTrigger.Builder.() -> Unit): Criterion {
        val trigger = StartedRidingTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun summonedEntity(name: String, init: SummonedEntityTrigger.Builder.() -> Unit): Criterion {
        val trigger = SummonedEntityTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun tameAnimal(name: String, init: TameAnimalTrigger.Builder.() -> Unit): Criterion {
        val trigger = TameAnimalTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun targetHit(name: String, init: TargetHitTrigger.Builder.() -> Unit): Criterion {
        val trigger = TargetHitTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun thrownItemPickedUpByEntity(name: String, init: ThrownItemPickedUpByEntityTrigger.Builder.() -> Unit): Criterion {
        val trigger = ThrownItemPickedUpByEntityTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun thrownItemPickedUpByPlayer(name: String, init: ThrownItemPickedUpByPlayerTrigger.Builder.() -> Unit): Criterion {
        val trigger = ThrownItemPickedUpByPlayerTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun tick(name: String, init: TickTrigger.Builder.() -> Unit): Criterion {
        val trigger = TickTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun usedEnderEye(name: String, init: UsedEnderEyeTrigger.Builder.() -> Unit): Criterion {
        val trigger = UsedEnderEyeTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun usedTotem(name: String, init: UsedTotemTrigger.Builder.() -> Unit): Criterion {
        val trigger = UsedTotemTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun villagerTrade(name: String, init: VillagerTradeTrigger.Builder.() -> Unit): Criterion {
        val trigger = VillagerTradeTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun voluntaryExile(name: String, init: VoluntaryExileTrigger.Builder.() -> Unit): Criterion {
        val trigger = VoluntaryExileTrigger.Builder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    internal fun build(): List<Criterion> {
        return criteria
    }
    
}