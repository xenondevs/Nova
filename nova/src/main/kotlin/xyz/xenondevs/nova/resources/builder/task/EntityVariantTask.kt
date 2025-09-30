package xyz.xenondevs.nova.resources.builder.task

import net.minecraft.resources.ResourceKey
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.layout.entity.EntityVariantLayout
import xyz.xenondevs.nova.resources.lookup.ResourceLookups

/**
 * Generates entity variant assets.
 */
class EntityVariantTask(private val builder: ResourcePackBuilder) : PackTask {
    
    override val stage = BuildStage.PRE_WORLD
    
    companion object {
        
        private val layoutGenerators = HashMap<ResourceKey<*>, (ResourcePackBuilder) -> EntityVariantLayout>()
        
        internal fun queueVariantAssetGeneration(key: ResourceKey<*>, makeLayout: (ResourcePackBuilder) -> EntityVariantLayout) { 
            layoutGenerators[key] = makeLayout 
        }
        
    }
    
    override suspend fun run() {
        val layouts: Map<ResourceKey<*>, EntityVariantLayout> = layoutGenerators
            .mapValues { (_, makeLayout) -> makeLayout(builder) }
        ResourceLookups.ENTITY_VARIANT_ASSETS_LOOKUP.set(layouts)
    }
    
}