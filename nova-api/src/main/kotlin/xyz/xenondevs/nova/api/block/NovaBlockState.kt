package xyz.xenondevs.nova.api.block

import org.bukkit.Location

interface NovaBlockState {
    
    /**
     * The material of this [NovaBlockState].
     */
    val material: NovaBlock
    
    /**
     * The location of this [NovaBlockState].
     */
    val location: Location
    
}