package xyz.xenondevs.nova.material

import de.studiocode.invui.item.builder.ItemBuilder
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.world.block.property.BlockPropertyType
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.impl.TileEntityContext
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import xyz.xenondevs.nova.world.block.model.BlockModelProviderType

typealias ItemBuilderModifierFun = (ItemBuilder, TileEntity?) -> ItemBuilder
typealias TileEntityConstructor = ((NovaTileEntityState) -> TileEntity)

@Suppress("UNCHECKED_CAST")
class TileEntityNovaMaterial internal constructor(
    id: NamespacedId,
    localizedName: String,
    novaItem: NovaItem?,
    novaBlock: NovaBlock<NovaTileEntityState>,
    options: BlockOptions,
    internal val tileEntityConstructor: TileEntityConstructor,
    modelProvider: BlockModelProviderType<*>,
    properties: List<BlockPropertyType<*>>,
    placeCheck: PlaceCheckFun?,
    multiBlockLoader: MultiBlockLoader?
) : BlockNovaMaterial(
    id,
    localizedName,
    novaItem,
    novaBlock as NovaBlock<NovaBlockState>, // fixme: users could cast to BlockNovaMaterial and then call methods on the NovaBlock with a BlockState that is not a NovaTileEntityState
    options,
    modelProvider,
    properties,
    placeCheck,
    multiBlockLoader
) {
    
    /**
     * Creates an [ItemBuilder][ItemBuilder] for this [ItemNovaMaterial].
     *
     * The [TileEntity] provided must be of the same type as the [TileEntity]
     * returned by the [tileEntityConstructor] function.
     */
    fun createItemBuilder(tileEntity: TileEntity): ItemBuilder {
        return modifyItemBuilder(createBasicItemBuilder(), TileEntityContext(tileEntity))
    }
    
    /**
     * Creates an [ItemStack] for this [ItemNovaMaterial].
     */
    fun createItemStack(amount: Int = 1, tileEntity: TileEntity): ItemStack =
        createItemBuilder(tileEntity).setAmount(amount).get()
    
    override fun createBlockState(pos: BlockPos): NovaTileEntityState =
        NovaTileEntityState(pos, this)
    
    override fun createNewBlockState(ctx: BlockPlaceContext): NovaTileEntityState =
        NovaTileEntityState(this, ctx)
    
}