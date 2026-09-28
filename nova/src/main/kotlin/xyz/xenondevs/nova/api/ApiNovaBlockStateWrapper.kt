@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.api

import com.mojang.datafixers.util.Either
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.craftbukkit.block.CraftBlockType
import xyz.xenondevs.nova.api.material.NovaMaterial
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.NovaBlockState
import xyz.xenondevs.nova.world.block.blockType
import xyz.xenondevs.nova.api.block.NovaBlock as INovaBlock
import xyz.xenondevs.nova.api.block.NovaBlockState as INovaBlockState

internal class ApiNovaBlockStateWrapper(
    private val block: Block,
    private val state: NovaBlockState
) : INovaBlockState {
    
    @Deprecated("Use NovaBlock instead", replaceWith = ReplaceWith("block"))
    override fun getMaterial(): NovaMaterial = LegacyMaterialWrapper(Either.right((state.blockType as CraftBlockType<*>).handle as NovaBlock))
    override fun getBlock(): INovaBlock = ApiBlockWrapper((state.blockType as CraftBlockType<*>).handle as NovaBlock)
    override fun getLocation(): Location = block.location
    
}
