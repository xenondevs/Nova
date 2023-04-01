package xyz.xenondevs.nova.api

import org.bukkit.Location
import xyz.xenondevs.nova.api.block.NovaBlock
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.api.block.NovaBlockState as INovaBlockStae

internal class ApiNovaBlockStateWrapper(private val state: NovaBlockState): INovaBlockStae {
    
    /**
     * The material of this [NovaBlockState].
     */
    override val material: NovaBlock = ApiBlockWrapper(state.material)
    
    /**
     * The location of this [NovaBlockState].
     */
    override val location: Location = state.location
    
}