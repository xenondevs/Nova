package xyz.xenondevs.nova.data.resources.builder.content

import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.AssetPack
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder

internal interface PackContent {
    
    /**
     * The resource pack building stage at which [includePack] and [write] will be called.
     */
    val stage: ResourcePackBuilder.BuildingStage
    
    /**
     * Initializes this [PackContent].
     */
    fun init() = Unit
    
    /**
     * Checks if the given [path] is excluded due to this pack content.
     */
    fun excludesPath(path: ResourcePath): Boolean = false
    
    /**
     * Writes all data related to the [pack] in the build dir.
     */
    fun includePack(pack: AssetPack)
    
    /**
     * Writes remaining data to the build dir.
     * 
     * This function is run after [includePack] has been called for all asset packs.
     */
    fun write()
    
}