@file:Suppress("unused", "UNCHECKED_CAST")

package xyz.xenondevs.nova.world.block

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.config.ConfigurableRegistryElementBuilder
import xyz.xenondevs.nova.data.world.block.property.BlockPropertyType
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.item.options.BlockOptions
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.name

private val EMPTY_BLOCK_OPTIONS = BlockOptions(0.0)

abstract class AbstractNovaBlockBuilder<S : AbstractNovaBlockBuilder<S, T, B>, T : NovaBlockState, B : NovaBlock> internal constructor(
    id: ResourceLocation
) : ConfigurableRegistryElementBuilder<S, B>(NovaRegistries.BLOCK, id) {
    
    internal constructor(addon: Addon, name: String) : this(ResourceLocation(addon, name))
    
    protected var localizedName = "block.${id.namespace}.${id.name}"
    protected abstract var logic: MutableList<BlockBehavior<T>>
    protected val properties = ArrayList<BlockPropertyType<*>>()
    protected var options: BlockOptions = EMPTY_BLOCK_OPTIONS
    protected var placeCheck: PlaceCheckFun? = null
    protected var multiBlockLoader: MultiBlockLoader? = null
    
    open fun behaviors(vararg behaviors: BlockBehavior<T>): S {
        this.logic = behaviors.toMutableList()
        return this as S
    }
    
    open fun addBehaviors(vararg block: BlockBehavior<T>): S {
        this.logic += block
        return this as S
    }
    
    fun properties(vararg properties: BlockPropertyType<*>): S {
        this.properties += properties
        return this as S
    }
    
    fun blockOptions(options: BlockOptions): S {
        this.options = options
        return this as S
    }
    
    fun placeCheck(placeCheck: PlaceCheckFun): S {
        this.placeCheck = placeCheck
        return this as S
    }
    
    fun multiBlockLoader(multiBlockLoader: MultiBlockLoader): S {
        this.multiBlockLoader = multiBlockLoader
        return this as S
    }
    
}

class NovaBlockBuilder internal constructor(id: ResourceLocation) : AbstractNovaBlockBuilder<NovaBlockBuilder, NovaBlockState, NovaBlock>(id) {
    
    internal constructor(addon: Addon, name: String) : this(ResourceLocation(addon, name))
    
    override var logic: MutableList<BlockBehavior<NovaBlockState>> = mutableListOf(BlockBehavior.Default)
    
    override fun build() = NovaBlock(
        id,
        localizedName,
        BlockLogic(logic),
        options,
        properties,
        placeCheck,
        multiBlockLoader,
        configId
    )
    
}

class TileEntityNovaBlockBuilder internal constructor(
    id: ResourceLocation,
    private val tileEntity: TileEntityConstructor
) : AbstractNovaBlockBuilder<TileEntityNovaBlockBuilder, NovaTileEntityState, NovaTileEntityBlock>(id) {
    
    internal constructor(
        addon: Addon,
        name: String,
        tileEntity: TileEntityConstructor
    ) : this(ResourceLocation(addon, name), tileEntity)
    
    override var logic: MutableList<BlockBehavior<NovaTileEntityState>> = mutableListOf(TileEntityBlockBehavior.INTERACTIVE)
    
    fun interactive(interactive: Boolean): TileEntityNovaBlockBuilder {
        logic.removeIf { it is TileEntityBlockBehavior }
        logic += if (interactive) TileEntityBlockBehavior.INTERACTIVE else TileEntityBlockBehavior.NON_INTERACTIVE
        return this
    }
    
    override fun build() = NovaTileEntityBlock(
        id,
        localizedName,
        BlockLogic(logic),
        options,
        tileEntity,
        properties,
        placeCheck,
        multiBlockLoader,
        configId
    )
    
}