package xyz.xenondevs.nova.api.block

import org.bukkit.Location
import xyz.xenondevs.nova.api.material.NovaMaterial

@Suppress("DEPRECATION")
interface NovaBlockState {
    
    /**
     * The material of this [NovaBlockState].
     */
    @Deprecated("Use NovaBlock instead", ReplaceWith("block"))
    val material: NovaMaterial
    
    /**
     * The block type of this [NovaBlockState].
     */
    val block: NovaBlock
    
    /**
     * The location of this [NovaBlockState].
     */
    val location: Location
    
}