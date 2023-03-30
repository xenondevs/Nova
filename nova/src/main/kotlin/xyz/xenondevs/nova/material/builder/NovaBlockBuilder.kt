@file:Suppress("unused", "UNCHECKED_CAST")

package xyz.xenondevs.nova.material.builder

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.world.block.property.BlockPropertyType
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.material.MultiBlockLoader
import xyz.xenondevs.nova.material.NovaBlock
import xyz.xenondevs.nova.material.NovaTileEntityBlock
import xyz.xenondevs.nova.material.PlaceCheckFun
import xyz.xenondevs.nova.material.TileEntityConstructor
import xyz.xenondevs.nova.material.options.BlockOptions
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.RegistryElementBuilder
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.name
import xyz.xenondevs.nova.world.block.BlockBehavior
import xyz.xenondevs.nova.world.block.BlockLogic
import xyz.xenondevs.nova.world.block.TileEntityBlockBehavior

private val EMPTY_BLOCK_OPTIONS = BlockOptions(0.0)

abstract class AbstractNovaBlockBuilder<S : AbstractNovaBlockBuilder<S, T, B>, T : NovaBlockState, B : NovaBlock> internal constructor(
    id: ResourceLocation
) : RegistryElementBuilder<B>(NovaRegistries.BLOCK, id) {
    
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
    
    override fun build(): NovaBlock {
        return NovaBlock(
            id,
            localizedName,
            BlockLogic(logic),
            options,
            properties,
            placeCheck,
            multiBlockLoader
        )
    }
    
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
    
    override fun build(): NovaTileEntityBlock {
        return NovaTileEntityBlock(
            id,
            localizedName,
            BlockLogic(logic),
            options,
            tileEntity,
            properties,
            placeCheck,
            multiBlockLoader
        )
        
    }
    
}