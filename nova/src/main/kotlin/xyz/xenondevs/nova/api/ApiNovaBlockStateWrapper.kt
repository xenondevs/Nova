package xyz.xenondevs.nova.api

import com.mojang.datafixers.util.Either
import org.bukkit.Location
import xyz.xenondevs.nova.api.block.NovaBlock
import xyz.xenondevs.nova.api.material.NovaMaterial
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.api.block.NovaBlockState as INovaBlockStae

@Suppress("DEPRECATION")
internal class ApiNovaBlockStateWrapper(private val state: NovaBlockState): INovaBlockStae {
    
    @Deprecated("Use NovaBlock instead", replaceWith = ReplaceWith("block"))
    override val material: NovaMaterial get() = LegacyMaterialWrapper(Either.right(state.block))
    override val block: NovaBlock get() = ApiBlockWrapper(state.block)
    override val location: Location = state.location
    
}