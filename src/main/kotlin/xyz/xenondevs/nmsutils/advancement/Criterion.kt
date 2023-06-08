package xyz.xenondevs.nmsutils.advancement

import net.minecraft.advancements.CriterionTriggerInstance
import net.minecraft.advancements.critereon.ImpossibleTrigger
import xyz.xenondevs.nmsutils.advancement.trigger.AllayDropItemOnBlockTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.AvoidVibrationTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.BeeNestDestroyedTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.BredAnimalsTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.BrewedPotionTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.ChangeDimensionTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.ChanneledLightningTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.ConstructBeaconTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.ConsumeItemTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.CuredZombieVillagerTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.EffectsChangedTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.EnchantedItemTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.EnterBlockTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.EntityHurtPlayerTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.EntityKilledPlayerTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.FallFromHeightTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.FilledBucketTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.FishingRodHookedTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.HeroOfTheVillageTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.InventoryChangedTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.ItemDurabilityChangedTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.ItemUsedOnBlockTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.KillMobNearSculkCatalystTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.KilledByCrossbowTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.LevitationTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.LightningStrikeTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.LocationTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.NetherTravelTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.PlacedBlockTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.PlayerGeneratesContainerLootTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.PlayerHurtEntityTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.PlayerInteractTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.PlayerKilledEntityTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.RecipeCraftedTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.RecipeUnlockedTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.RideEntityInLavaTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.ShotCrossbowTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.SleptInBedTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.SlideDownBlockTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.StartedRidingTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.SummonedEntityTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.TameAnimalTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.TargetBlockTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.ThrownItemPickedUpByEntityTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.ThrownItemPickedUpByPlayerTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.TickTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.TradeTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.UsedEnderEyeTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.UsedTotemTriggerBuilder
import xyz.xenondevs.nmsutils.advancement.trigger.VoluntaryExileTriggerBuilder
import net.minecraft.advancements.Criterion as MojangCriterion

data class Criterion(
    val name: String,
    val trigger: CriterionTriggerInstance?
) {
    
    internal fun toNMS(): MojangCriterion =
        MojangCriterion(trigger)
    
}

@AdvancementDsl
class CriteriaBuilder {
    
    private val criteria = ArrayList<Criterion>()
    
    fun criterion(name: String, trigger: CriterionTriggerInstance): Criterion {
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun allayDropItemOnBlock(name: String, init: AllayDropItemOnBlockTriggerBuilder.() -> Unit): Criterion {
        val trigger = AllayDropItemOnBlockTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun avoidVibration(name: String, init: AvoidVibrationTriggerBuilder.() -> Unit): Criterion {
        val trigger = AvoidVibrationTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun beeNestDestroyed(name: String, init: BeeNestDestroyedTriggerBuilder.() -> Unit): Criterion {
        val trigger = BeeNestDestroyedTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun bredAnimals(name: String, init: BredAnimalsTriggerBuilder.() -> Unit): Criterion {
        val trigger = BredAnimalsTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun brewedPotion(name: String, init: BrewedPotionTriggerBuilder.() -> Unit): Criterion {
        val trigger = BrewedPotionTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun changedDimension(name: String, init: ChangeDimensionTriggerBuilder.() -> Unit): Criterion {
        val trigger = ChangeDimensionTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun channeledLightning(name: String, init: ChanneledLightningTriggerBuilder.() -> Unit): Criterion {
        val trigger = ChanneledLightningTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun constructBeacon(name: String, init: ConstructBeaconTriggerBuilder.() -> Unit): Criterion {
        val trigger = ConstructBeaconTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun consumeItem(name: String, init: ConsumeItemTriggerBuilder.() -> Unit): Criterion {
        val trigger = ConsumeItemTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun curedZombieVillager(name: String, init: CuredZombieVillagerTriggerBuilder.() -> Unit): Criterion {
        val trigger = CuredZombieVillagerTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun effectsChanged(name: String, init: EffectsChangedTriggerBuilder.() -> Unit): Criterion {
        val trigger = EffectsChangedTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun enchantedItem(name: String, init: EnchantedItemTriggerBuilder.() -> Unit): Criterion {
        val trigger = EnchantedItemTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun enterBlock(name: String, init: EnterBlockTriggerBuilder.() -> Unit): Criterion {
        val trigger = EnterBlockTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun entityHurtPlayer(name: String, init: EntityHurtPlayerTriggerBuilder.() -> Unit): Criterion {
        val trigger = EntityHurtPlayerTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun entityKilledPlayer(name: String, init: EntityKilledPlayerTriggerBuilder.() -> Unit): Criterion {
        val trigger = EntityKilledPlayerTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun fallFromHeight(name: String, init: FallFromHeightTriggerBuilder.() -> Unit): Criterion {
        val trigger = FallFromHeightTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun filledBucket(name: String, init: FilledBucketTriggerBuilder.() -> Unit): Criterion {
        val trigger = FilledBucketTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun fishingRodHooked(name: String, init: FishingRodHookedTriggerBuilder.() -> Unit): Criterion {
        val trigger = FishingRodHookedTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun heroOfTheVillage(name: String, init: HeroOfTheVillageTriggerBuilder.() -> Unit): Criterion {
        val trigger = HeroOfTheVillageTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun impossible(): Criterion {
        return Criterion("impossible", ImpossibleTrigger.TriggerInstance()).apply(criteria::add)
    }
    
    fun inventoryChanged(name: String, init: InventoryChangedTriggerBuilder.() -> Unit): Criterion {
        val trigger = InventoryChangedTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun itemDurabilityChanged(name: String, init: ItemDurabilityChangedTriggerBuilder.() -> Unit): Criterion {
        val trigger = ItemDurabilityChangedTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun itemUsedOnBlock(name: String, init: ItemUsedOnBlockTriggerBuilder.() -> Unit): Criterion {
        val trigger = ItemUsedOnBlockTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun killMobNearSculkCatalyst(name: String, init: KillMobNearSculkCatalystTriggerBuilder.() -> Unit): Criterion {
        val trigger = KillMobNearSculkCatalystTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun killedByCrossbow(name: String, init: KilledByCrossbowTriggerBuilder.() -> Unit): Criterion {
        val trigger = KilledByCrossbowTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun levitation(name: String, init: LevitationTriggerBuilder.() -> Unit): Criterion {
        val trigger = LevitationTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun lightningStrike(name: String, init: LightningStrikeTriggerBuilder.() -> Unit): Criterion {
        val trigger = LightningStrikeTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun location(name: String, init: LocationTriggerBuilder.() -> Unit): Criterion {
        val trigger = LocationTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun netherTravel(name: String, init: NetherTravelTriggerBuilder.() -> Unit): Criterion {
        val trigger = NetherTravelTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun placedBlock(name: String, init: PlacedBlockTriggerBuilder.() -> Unit): Criterion {
        val trigger = PlacedBlockTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun playerGeneratesContainerLoot(name: String, init: PlayerGeneratesContainerLootTriggerBuilder.() -> Unit): Criterion {
        val trigger = PlayerGeneratesContainerLootTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun playerHurtEntity(name: String, init: PlayerHurtEntityTriggerBuilder.() -> Unit): Criterion {
        val trigger = PlayerHurtEntityTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun playerInteractedWithEntity(name: String, init: PlayerInteractTriggerBuilder.() -> Unit): Criterion {
        val trigger = PlayerInteractTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun playerKilledEntity(name: String, init: PlayerKilledEntityTriggerBuilder.() -> Unit): Criterion {
        val trigger = PlayerKilledEntityTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun recipeCrafted(name: String, init: RecipeCraftedTriggerBuilder.() -> Unit): Criterion {
        val trigger = RecipeCraftedTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun recipeUnlocked(name: String, init: RecipeUnlockedTriggerBuilder.() -> Unit): Criterion {
        val trigger = RecipeUnlockedTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun rideEntityInLava(name: String, init: RideEntityInLavaTriggerBuilder.() -> Unit): Criterion {
        val trigger = RideEntityInLavaTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun shotCrossbow(name: String, init: ShotCrossbowTriggerBuilder.() -> Unit): Criterion {
        val trigger = ShotCrossbowTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun sleptInBed(name: String, init: SleptInBedTriggerBuilder.() -> Unit): Criterion {
        val trigger = SleptInBedTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun slideDownBlock(name: String, init: SlideDownBlockTriggerBuilder.() -> Unit): Criterion {
        val trigger = SlideDownBlockTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun startedRiding(name: String, init: StartedRidingTriggerBuilder.() -> Unit): Criterion {
        val trigger = StartedRidingTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun summonedEntity(name: String, init: SummonedEntityTriggerBuilder.() -> Unit): Criterion {
        val trigger = SummonedEntityTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun tameAnimal(name: String, init: TameAnimalTriggerBuilder.() -> Unit): Criterion {
        val trigger = TameAnimalTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun targetHit(name: String, init: TargetBlockTriggerBuilder.() -> Unit): Criterion {
        val trigger = TargetBlockTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun thrownItemPickedUpByEntity(name: String, init: ThrownItemPickedUpByEntityTriggerBuilder.() -> Unit): Criterion {
        val trigger = ThrownItemPickedUpByEntityTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun thrownItemPickedUpByPlayer(name: String, init: ThrownItemPickedUpByPlayerTriggerBuilder.() -> Unit): Criterion {
        val trigger = ThrownItemPickedUpByPlayerTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun tick(name: String, init: TickTriggerBuilder.() -> Unit): Criterion {
        val trigger = TickTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun usedEnderEye(name: String, init: UsedEnderEyeTriggerBuilder.() -> Unit): Criterion {
        val trigger = UsedEnderEyeTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun usedTotem(name: String, init: UsedTotemTriggerBuilder.() -> Unit): Criterion {
        val trigger = UsedTotemTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun villagerTrade(name: String, init: TradeTriggerBuilder.() -> Unit): Criterion {
        val trigger = TradeTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    fun voluntaryExile(name: String, init: VoluntaryExileTriggerBuilder.() -> Unit): Criterion {
        val trigger = VoluntaryExileTriggerBuilder().apply(init).build()
        return Criterion(name, trigger).apply(criteria::add)
    }
    
    internal fun build(): List<Criterion> {
        return criteria
    }
    
}