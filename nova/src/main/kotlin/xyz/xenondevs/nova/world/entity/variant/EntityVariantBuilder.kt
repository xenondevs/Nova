package xyz.xenondevs.nova.world.entity.variant

import net.kyori.adventure.key.Key
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.variant.SpawnPrioritySelectors
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.registry.LazyRegistryElementBuilder
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.layout.entity.EntityVariantLayout
import xyz.xenondevs.nova.resources.builder.layout.entity.EntityVariantLayoutBuilder
import xyz.xenondevs.nova.resources.builder.task.EntityVariantTask
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.util.toIdentifier

abstract class EntityVariantBuilder<
    T : Any,
    M : Any,
    NMS : Any,
    L : EntityVariantLayout,
    LB : EntityVariantLayoutBuilder<L>
    >
internal constructor(
    registryKey: ResourceKey<Registry<NMS>>,
    nmsToBukkit: (Holder<NMS>) -> T,
    private val defaultModelType: M,
    private val makeLayoutBuilder: (namespace: String, ResourcePackBuilder) -> LB,
    id: Key
) : LazyRegistryElementBuilder<T, NMS>(registryKey, nmsToBukkit, id) {
    
    private var configureSpawnConditions: SpawnConditionsBuilder.() -> Unit = {}
    private var modelType: M = defaultModelType
    
    /**
     * Configures the [conditions](https://minecraft.wiki/w/Mob_variant_definitions#Spawn_condition) under which this entity variant can spawn.
     * 
     * When spawning, the selection of a variant happens in 3 steps:
     * 1. Each condition is evaluated. The matching condition with the highest priority determines the priority of the variant. If no condition matches the variant is discarded.
     * 2. Of all remaining variants, all except those with the highest priority are discarded.
     * 3. Of the remaining variants, a random variant is chosen.
     */
    fun spawnConditions(spawnConditions: SpawnConditionsBuilder.() -> Unit) {
        this.configureSpawnConditions = spawnConditions
    }
    
    /**
     * Configures the [modelType] and [texture] of the entity variant.
     */
    fun texture(modelType: M = defaultModelType, texture: LB.() -> Unit) {
        this.modelType = modelType
        val key = ResourceKey.create(registryKey, id.toIdentifier())
        EntityVariantTask.queueVariantAssetGeneration(key) {
            makeLayoutBuilder(id.namespace(), it)
                .apply(texture)
                .build()
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    final override fun build(lookup: RegistryOps.RegistryInfoLookup): NMS {
        val key = ResourceKey.create(registryKey, id.toIdentifier())
        val layout = ResourceLookups.ENTITY_VARIANT_ASSETS[key]
            ?: throw IllegalStateException("Missing variant assets for $id in lookup")
        val spawnConditions = SpawnConditionsBuilder(lookup).apply(configureSpawnConditions).build()
        val variant = build(modelType, layout as L, spawnConditions)
        return variant
    }
    
    override fun register(): Provider<T> {
        val key = ResourceKey.create(registryKey, id.toIdentifier())
        UnknownEntityVariants.rememberEntityVariantKey(key)
        return super.register()
    }
    
    internal abstract fun build(modelType: M, layout: L, spawnConditions: SpawnPrioritySelectors): NMS
    
}