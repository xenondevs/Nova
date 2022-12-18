@file:Suppress("unused")

package xyz.xenondevs.nova.material.builder

import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.world.block.property.BlockPropertyType
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.impl.TileEntityItemBehavior
import xyz.xenondevs.nova.material.BlockNovaMaterial
import xyz.xenondevs.nova.material.MultiBlockLoader
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.material.PlaceCheckFun
import xyz.xenondevs.nova.material.TileEntityConstructor
import xyz.xenondevs.nova.material.TileEntityNovaMaterial
import xyz.xenondevs.nova.material.options.BlockOptions
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.TileEntityBlock

private val EMPTY_BLOCK_OPTIONS = BlockOptions(0.0)

abstract class AbstractBlockNovaMaterialBuilder<S : AbstractBlockNovaMaterialBuilder<S, T>, T : NovaBlockState> internal constructor(addon: Addon, name: String) : NovaMaterialBuilder<S>(addon, name) {
    
    override var localizedName = "block.${id.namespace}.$name"
    protected abstract var block: NovaBlock<T>
    protected val properties = ArrayList<BlockPropertyType<*>>()
    protected var options: BlockOptions = EMPTY_BLOCK_OPTIONS
    protected var placeCheck: PlaceCheckFun? = null
    protected var multiBlockLoader: MultiBlockLoader? = null
    
    open fun block(block: NovaBlock<T>): S {
        this.block = block
        return getThis()
    }
    
    fun properties(vararg properties: BlockPropertyType<*>): S {
        this.properties += properties
        return getThis()
    }
    
    fun blockOptions(options: BlockOptions): S {
        this.options = options
        return getThis()
    }
    
    fun placeCheck(placeCheck: PlaceCheckFun): S {
        this.placeCheck = placeCheck
        return getThis()
    }
    
    fun multiBlockLoader(multiBlockLoader: MultiBlockLoader): S {
        this.multiBlockLoader = multiBlockLoader
        return getThis()
    }
    
    abstract override fun register(): BlockNovaMaterial
    
}

class BlockNovaMaterialBuilder internal constructor(addon: Addon, name: String) : AbstractBlockNovaMaterialBuilder<BlockNovaMaterialBuilder, NovaBlockState>(addon, name) {
    
    override var block: NovaBlock<NovaBlockState> = NovaBlock.Default
    
    override fun getThis(): BlockNovaMaterialBuilder {
        return this
    }
    
    override fun register(): BlockNovaMaterial {
        return NovaMaterialRegistry.register(
            BlockNovaMaterial(
                id,
                localizedName,
                NovaItem(itemBehaviors),
                maxStackSize,
                block,
                options,
                properties,
                placeCheck,
                multiBlockLoader
            )
        )
    }
    
}

class TileEntityNovaMaterialBuilder internal constructor(
    addon: Addon,
    name: String,
    private val tileEntity: TileEntityConstructor
) : AbstractBlockNovaMaterialBuilder<TileEntityNovaMaterialBuilder, NovaTileEntityState>(addon, name) {
    
    override var block: NovaBlock<NovaTileEntityState> = TileEntityBlock.INTERACTIVE
    
    init {
        itemBehaviors(TileEntityItemBehavior())
    }
    
    fun interactive(interactive: Boolean): TileEntityNovaMaterialBuilder {
        block = if (interactive) TileEntityBlock.INTERACTIVE else TileEntityBlock.NON_INTERACTIVE
        return this
    }
    
    fun lore(lore: Boolean) {
        itemBehaviors.removeIf { it is TileEntityItemBehavior }
        if (lore)
            itemBehaviors += TileEntityItemBehavior()
    }
    
    override fun getThis(): TileEntityNovaMaterialBuilder {
        return this
    }
    
    override fun register(): TileEntityNovaMaterial {
        return NovaMaterialRegistry.register(
            TileEntityNovaMaterial(
                id,
                localizedName,
                NovaItem(itemBehaviors),
                maxStackSize,
                block,
                options,
                tileEntity,
                properties,
                placeCheck,
                multiBlockLoader
            )
        )
    }
    
}