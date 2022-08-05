package xyz.xenondevs.nova.api.block

import org.bukkit.Location
import xyz.xenondevs.nova.api.material.NovaMaterial

interface NovaBlockState {
    
    /**
     * The material of this [NovaBlockState].
     */
    val material: NovaMaterial
    
    /**
     * The location of this [NovaBlockState].
     */
    val location: Location
    
}