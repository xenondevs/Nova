package xyz.xenondevs.nova.world.entity.variant

import kotlinx.serialization.builtins.SetSerializer
import net.minecraft.core.ClientAsset
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.animal.ChickenVariant
import net.minecraft.world.entity.animal.CowVariant
import net.minecraft.world.entity.animal.PigVariant
import net.minecraft.world.entity.animal.frog.FrogVariant
import net.minecraft.world.entity.animal.wolf.WolfVariant
import net.minecraft.world.entity.variant.ModelAndTexture
import net.minecraft.world.entity.variant.SpawnPrioritySelectors
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.patch.impl.registry.preFreeze
import xyz.xenondevs.nova.resources.ResourceGeneration
import xyz.xenondevs.nova.serialization.kotlinx.ResourceKeySerializer
import xyz.xenondevs.nova.util.set

private const val STORAGE_ID = "custom_entity_variant_keys"

@InternalInit(
    stage = InternalInitStage.PRE_WORLD,
    dependsOn = [ResourceGeneration.PreWorld::class]
)
internal object UnknownEntityVariants {
    
    private val customVariantKeys: HashSet<ResourceKey<*>> = PermanentStorage.retrieve(
        STORAGE_ID,
        SetSerializer(ResourceKeySerializer),
    )?.toHashSet() ?: HashSet()
    private val registeredIds = HashSet<ResourceKey<*>>()
    
    @InitFun
    private fun addUnknownEntityVariants() {
        PermanentStorage.store(STORAGE_ID, SetSerializer(ResourceKeySerializer), customVariantKeys)
        
        val unregisteredIds = customVariantKeys - registeredIds
        for (key in unregisteredIds) {
            val registryKey = key.registryKey()
            val id = key.location()
            when (registryKey) {
                Registries.CHICKEN_VARIANT -> Registries.CHICKEN_VARIANT.preFreeze { registry, _ ->
                    registry[id] = ChickenVariant(
                        ModelAndTexture(ChickenVariant.ModelType.NORMAL, id),
                        SpawnPrioritySelectors.EMPTY
                    )
                }
                
                Registries.COW_VARIANT -> Registries.COW_VARIANT.preFreeze { registry, _ ->
                    registry[id] = CowVariant(
                        ModelAndTexture(CowVariant.ModelType.NORMAL, id),
                        SpawnPrioritySelectors.EMPTY
                    )
                }
                
                Registries.FROG_VARIANT -> Registries.FROG_VARIANT.preFreeze { registry, _ ->
                    registry[id] = FrogVariant(
                        ClientAsset(id),
                        SpawnPrioritySelectors.EMPTY
                    )
                }
                
                Registries.PIG_VARIANT -> Registries.PIG_VARIANT.preFreeze { registry, _ ->
                    registry[id] = PigVariant(
                        ModelAndTexture(PigVariant.ModelType.NORMAL, id),
                        SpawnPrioritySelectors.EMPTY
                    )
                }
                
                Registries.WOLF_VARIANT -> Registries.WOLF_VARIANT.preFreeze { registry, _ ->
                    registry[id] = WolfVariant(
                        WolfVariant.AssetInfo(
                            ClientAsset(id), 
                            ClientAsset(id),
                            ClientAsset(id)
                        ),
                        SpawnPrioritySelectors.EMPTY
                    )
                }
                
                else -> LOGGER.error("Unknown entity variant has unhandled entity type: $key")
            }
        }
    }
    
    fun rememberEntityVariantKey(id: ResourceKey<*>) {
        customVariantKeys += id
        registeredIds += id
    }
    
}