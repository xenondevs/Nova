package xyz.xenondevs.nova.resources.builder.task

import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.world.item.behavior.NoHandAnimationWhileHolding

/**
 * Generates extra item model definitions for items with the [NoHandAnimationWhileHolding] behavior.
 */
class NoHandAnimationTask(builder: ResourcePackBuilder) : PackTask {
    
    override val stage = BuildStage.POST_WORLD // accesses item registry
    override val runsAfter = setOf(ItemModelContent.GenerateItemDefinitions::class)
    override val runsBefore = setOf(ItemModelContent.Write::class)
    
    private val itemModelContent by builder.getBuildDataLazily<ItemModelContent>()
    
    override suspend fun run() {
        for (item in NovaRegistries.ITEM.entrySet.get()) {
            if (!item.hasBehavior<NoHandAnimationWhileHolding>())
                continue
            
            val path = ResourcePath.of(ResourceType.ItemModelDefinition, item.key)
            val noHandPath = ResourcePath(ResourceType.ItemModelDefinition, item.key.namespace(), item.key.value() + "_no_hand_animation")
            
            val normalModel = itemModelContent[path]
                ?: continue
            
            itemModelContent[noHandPath] = normalModel.copy(handAnimationOnSwap = false)
        }
    }
    
}