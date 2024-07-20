@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.api

import com.mojang.datafixers.util.Either
import org.bukkit.Location
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.api.block.NovaBlock as INovaBlock
import xyz.xenondevs.nova.api.block.NovaTileEntityState as INovaTileEntityState
import xyz.xenondevs.nova.api.material.NovaMaterial as INovaMaterial
import xyz.xenondevs.nova.api.tileentity.TileEntity as ITileEntity

internal class ApiNovaTileEntityStateWrapper(
    private val pos: BlockPos,
    private val state: NovaBlockState,
    private val tileEntity: TileEntity
) : INovaTileEntityState {
    
    @Deprecated("Use NovaBlock instead", replaceWith = ReplaceWith("block"))
    override fun getMaterial(): INovaMaterial = LegacyMaterialWrapper(Either.right(state.block))
    override fun getTileEntity(): ITileEntity = ApiTileEntityWrapper(tileEntity)
    override fun getBlock(): INovaBlock = ApiBlockWrapper(state.block)
    override fun getLocation(): Location = pos.location
    
}