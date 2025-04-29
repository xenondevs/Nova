package xyz.xenondevs.nova.resources.builder.task

import net.minecraft.resources.ResourceKey
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.layout.entity.EntityVariantLayout
import xyz.xenondevs.nova.resources.lookup.ResourceLookups

class EntityVariantContent internal constructor(
    private val builder: ResourcePackBuilder
) : PackTaskHolder {
    
    companion object {
        
        private val layoutGenerators = HashMap<ResourceKey<*>, (ResourcePackBuilder) -> EntityVariantLayout>()
        
        internal fun queueVariantAssetGeneration(key: ResourceKey<*>, makeLayout: (ResourcePackBuilder) -> EntityVariantLayout) { 
            layoutGenerators[key] = makeLayout 
        }
        
    }
    
    @PackTask(runAfter = ["ExtractTask#extractAll"])
    private fun write() {
        val layouts: Map<ResourceKey<*>, EntityVariantLayout> = layoutGenerators
            .mapValues { (_, makeLayout) -> makeLayout(builder) }
        ResourceLookups.ENTITY_VARIANT_ASSETS_LOOKUP.set(layouts)
    }
    
}