package xyz.xenondevs.nova.material

import com.mojang.serialization.Codec
import net.minecraft.resources.ResourceLocation
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.model.data.BlockModelData
import xyz.xenondevs.nova.data.resources.model.data.BlockStateBlockModelData
import xyz.xenondevs.nova.data.resources.model.data.DisplayEntityBlockModelData
import xyz.xenondevs.nova.data.world.block.property.BlockPropertyType
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.material.options.BlockOptions
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.BlockLogic
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import java.util.concurrent.CompletableFuture

typealias PlaceCheckFun = ((Player, ItemStack, Location) -> CompletableFuture<Boolean>)
typealias MultiBlockLoader = (BlockPos) -> List<BlockPos>

open class NovaBlock internal constructor(
    val id: ResourceLocation,
    val localizedName: String,
    val blockLogic: BlockLogic<NovaBlockState>,
    val options: BlockOptions,
    val properties: List<BlockPropertyType<*>>,
    val placeCheck: PlaceCheckFun?,
    val multiBlockLoader: MultiBlockLoader?
) {
    
    var item: NovaItem? = null
    
    val block: BlockModelData by lazy { Resources.getModelData(id).block!! }
    
    internal val vanillaBlockMaterial: Material
        get() = when (val block = block) {
            is DisplayEntityBlockModelData -> block.hitboxType
            is BlockStateBlockModelData -> block[0].type.material
        }
    
    internal open fun createBlockState(pos: BlockPos): NovaBlockState =
        NovaBlockState(pos, this)
    
    internal open fun createNewBlockState(ctx: BlockPlaceContext): NovaBlockState =
        NovaBlockState(this, ctx)
    
    companion object {
        
        val CODEC: Codec<NovaBlock> = NovaRegistries.BLOCK.byNameCodec()
        
    }
    
}