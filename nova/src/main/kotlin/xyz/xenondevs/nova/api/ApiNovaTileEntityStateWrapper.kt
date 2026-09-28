@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.api

import com.mojang.datafixers.util.Either
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.craftbukkit.block.CraftBlockType
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.NovaBlockState
import xyz.xenondevs.nova.world.block.blockType
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.api.block.NovaBlock as INovaBlock
import xyz.xenondevs.nova.api.block.NovaTileEntityState as INovaTileEntityState
import xyz.xenondevs.nova.api.material.NovaMaterial as INovaMaterial
import xyz.xenondevs.nova.api.tileentity.TileEntity as ITileEntity

internal class ApiNovaTileEntityStateWrapper(
    private val block: Block,
    private val state: NovaBlockState,
    private val tileEntity: TileEntity
) : INovaTileEntityState {
    
    @Deprecated("Use NovaBlock instead", replaceWith = ReplaceWith("block"))
    override fun getMaterial(): INovaMaterial = LegacyMaterialWrapper(Either.right((state.blockType as CraftBlockType<*>).handle as NovaBlock))
    override fun getTileEntity(): ITileEntity = ApiTileEntityWrapper(tileEntity)
    override fun getBlock(): INovaBlock = ApiBlockWrapper((state.blockType as CraftBlockType<*>).handle as NovaBlock)
    override fun getLocation(): Location = block.location
    
}
