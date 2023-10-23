@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.api

import com.mojang.datafixers.util.Either
import org.bukkit.Location
import xyz.xenondevs.nova.api.block.NovaBlock
import xyz.xenondevs.nova.api.material.NovaMaterial
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.api.block.NovaBlockState as INovaBlockState

internal class ApiNovaBlockStateWrapper(private val state: NovaBlockState): INovaBlockState {
    
    @Deprecated("Use NovaBlock instead", replaceWith = ReplaceWith("block"))
    override fun getMaterial(): NovaMaterial = LegacyMaterialWrapper(Either.right(state.block))
    override fun getBlock(): NovaBlock = ApiBlockWrapper(state.block)
    override fun getLocation(): Location = state.location
    
}