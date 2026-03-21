package xyz.xenondevs.nova.resources.builder.task

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.layout.entity.EntityVariantLayout
import xyz.xenondevs.nova.resources.lookup.ResourceLookups

/**
 * Generates entity variant assets.
 */
class EntityVariantTask(private val builder: ResourcePackBuilder) : PackTask {
    
    override val stage = BuildStage.PRE_WORLD
    
    companion object {
        
        private val layoutGenerators = HashMap<Pair<Key, Key>, (ResourcePackBuilder) -> EntityVariantLayout>()
        
        internal fun queueVariantAssetGeneration(type: Key, key: Key, makeLayout: (ResourcePackBuilder) -> EntityVariantLayout) {
            layoutGenerators[type to key] = makeLayout
        }
        
    }
    
    override suspend fun run() {
        val layouts: Map<Pair<Key, Key>, EntityVariantLayout> = layoutGenerators
            .mapValues { (_, makeLayout) -> makeLayout(builder) }
        ResourceLookups.entityVariantAssetsLookup.set(layouts)
    }
    
}