package xyz.xenondevs.nova.api

import com.mojang.datafixers.util.Either
import org.bukkit.Location
import xyz.xenondevs.nova.api.block.NovaBlock
import xyz.xenondevs.nova.api.material.NovaMaterial
import xyz.xenondevs.nova.api.tileentity.TileEntity
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.api.block.NovaTileEntityState as INovaTileEntityState

@Suppress("DEPRECATION")
internal class ApiNovaTileEntityStateWrapper(private val state: NovaTileEntityState): INovaTileEntityState {
    
    @Deprecated("Use NovaBlock instead", replaceWith = ReplaceWith("block"))
    override val material: NovaMaterial get() = LegacyMaterialWrapper(Either.right(state.block))
    override val tileEntity: TileEntity get() = ApiTileEntityWrapper(state.tileEntity)
    override val block: NovaBlock get() = ApiBlockWrapper(state.block)
    override val location: Location get() = state.location
    
}