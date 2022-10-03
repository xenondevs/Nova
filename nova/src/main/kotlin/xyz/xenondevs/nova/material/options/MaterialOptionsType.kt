package xyz.xenondevs.nova.material.options

import xyz.xenondevs.nova.material.ItemNovaMaterial

internal interface MaterialOptionsType<T> {
    
    /**
     * Creates an instance of [T] that loads its values from the config of [material].
     */
    fun configurable(material: ItemNovaMaterial): T
    
    /**
     * Creates an instance of [T] that loads its values from the config under [path].
     */
    fun configurable(path: String): T
    
}

/**
 * It is generally recommended to make your material options configurable.
 * 
 * If you still want to hardcode your material options, you can use this annotation to opt-in.
 */
@RequiresOptIn
annotation class HardcodedMaterialOptions