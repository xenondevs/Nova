package xyz.xenondevs.nova.resources.builder.layout

import org.jetbrains.annotations.ApiStatus
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder

@ApiStatus.NonExtendable
interface ModelSelectorScope {
    
    /**
     * The default model of the block or item.
     */
    val defaultModel: ModelBuilder
    
    /**
     * Gets the model under the given [path] or throws an exception if it does not exist.
     */
    fun getModel(path: ResourcePath<ResourceType.Model>): ModelBuilder
    
    /**
     * Gets the model under the given [path] or throws an exception if it does not exist.
     */
    fun getModel(path: String): ModelBuilder
    
}