package xyz.xenondevs.nmsutils.advancement

import net.minecraft.advancements.CriterionTriggerInstance
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.trigger.*
import kotlin.reflect.KClass

internal object TriggerRegistry {
    
    private val ADAPTERS: Map<KClass<out Trigger>, Adapter<out Trigger, *>> = mapOf(
        AllayDropItemOnBlockTrigger::class to AllayDropItemOnBlockTrigger,
        AvoidVibrationTrigger::class to AvoidVibrationTrigger,
        BeeNestDestroyedTrigger::class to BeeNestDestroyedTrigger,
        BredAnimalsTrigger::class to BredAnimalsTrigger,
        BrewedPotionTrigger::class to BrewedPotionTrigger,
        ChangedDimensionTrigger::class to ChangedDimensionTrigger,
        ChanneledLightningTrigger::class to ChanneledLightningTrigger,
        ConstructBeaconTrigger::class to ConstructBeaconTrigger,
        ConsumeItemTrigger::class to ConsumeItemTrigger,
        CuredZombieVillagerTrigger::class to CuredZombieVillagerTrigger,
        EffectsChangedTrigger::class to EffectsChangedTrigger,
        EnchantedItemTrigger::class to EnchantedItemTrigger,
        EnterBlockTrigger::class to EnterBlockTrigger,
        EntityHurtPlayerTrigger::class to EntityHurtPlayerTrigger,
        EntityKilledPlayerTrigger::class to EntityKilledPlayerTrigger,
        FallFromHeightTrigger::class to FallFromHeightTrigger,
        FilledBucketTrigger::class to FilledBucketTrigger,
        FishingRodHookedTrigger::class to FishingRodHookedTrigger,
        HeroOfTheVillageTrigger::class to HeroOfTheVillageTrigger,
        ImpossibleTrigger::class to ImpossibleTrigger,
        InventoryChangedTrigger::class to InventoryChangedTrigger,
        ItemDurabilityChangedTrigger::class to ItemDurabilityChangedTrigger,
        ItemUsedOnBlockTrigger::class to ItemUsedOnBlockTrigger,
        KilledByCrossbowTrigger::class to KilledByCrossbowTrigger,
        KillMobNearSculkCatalystTrigger::class to KillMobNearSculkCatalystTrigger,
        LevitationTrigger::class to LevitationTrigger,
        LightningStrikeTrigger::class to LightningStrikeTrigger,
        LocationTrigger::class to LocationTrigger,
        NetherTravelTrigger::class to NetherTravelTrigger,
        PlacedBlockTrigger::class to PlacedBlockTrigger,
        PlayerGeneratesContainerLootTrigger::class to PlayerGeneratesContainerLootTrigger,
        PlayerHurtEntityTrigger::class to PlayerHurtEntityTrigger,
        PlayerInteractedWithEntityTrigger::class to PlayerInteractedWithEntityTrigger,
        PlayerKilledEntityTrigger::class to PlayerKilledEntityTrigger,
        RecipeUnlockedTrigger::class to RecipeUnlockedTrigger,
        RideEntityInLavaTrigger::class to RideEntityInLavaTrigger,
        ShotCrossbowTrigger::class to ShotCrossbowTrigger,
        SleptInBedTrigger::class to SleptInBedTrigger,
        SlideDownBlockTrigger::class to SlideDownBlockTrigger,
        StartedRidingTrigger::class to StartedRidingTrigger,
        SummonedEntityTrigger::class to SummonedEntityTrigger,
        TameAnimalTrigger::class to TameAnimalTrigger,
        TargetHitTrigger::class to TargetHitTrigger,
        ThrownItemPickedUpByEntityTrigger::class to ThrownItemPickedUpByEntityTrigger,
        ThrownItemPickedUpByPlayerTrigger::class to ThrownItemPickedUpByPlayerTrigger,
        TickTrigger::class to TickTrigger,
        UsedEnderEyeTrigger::class to UsedEnderEyeTrigger,
        UsedTotemTrigger::class to UsedTotemTrigger,
        UsingItemTrigger::class to UsingItemTrigger,
        VillagerTradeTrigger::class to VillagerTradeTrigger,
        VoluntaryExileTrigger::class to VoluntaryExileTrigger
    )
    
    @Suppress("UNCHECKED_CAST")
    fun <T : Trigger, R : CriterionTriggerInstance> toNMS(trigger: T): R {
        return (ADAPTERS[trigger::class] as Adapter<T, R>).toNMS(trigger)
    }
    
}