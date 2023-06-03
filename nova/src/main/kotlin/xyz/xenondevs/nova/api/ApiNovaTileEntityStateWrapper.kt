package xyz.xenondevs.nova.api

import com.mojang.datafixers.util.Either
import org.bukkit.Location
import xyz.xenondevs.nova.api.block.NovaBlock
import xyz.xenondevs.nova.api.material.NovaMaterial
import xyz.xenondevs.nova.api.tileentity.TileEntity
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.api.block.NovaTileEntityState as INovaTileEntityState

@Suppress("DEPRECATION")
internal class ApiNovaTileEntityStateWrapper(private val state: NovaTileEntityState) : INovaTileEntityState {
    
    @Deprecated("Use NovaBlock instead", replaceWith = ReplaceWith("block"))
    override fun getMaterial(): NovaMaterial = LegacyMaterialWrapper(Either.right(state.block))
    override fun getTileEntity(): TileEntity = ApiTileEntityWrapper(state.tileEntity)
    override fun getBlock(): NovaBlock = ApiBlockWrapper(state.block)
    override fun getLocation(): Location = state.location
    
}