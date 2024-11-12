@file:Suppress("unused")

package xyz.xenondevs.nova.world.block

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.config.ConfigurableRegistryElementBuilder
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.layout.block.BlockModelLayout
import xyz.xenondevs.nova.resources.layout.block.BlockModelLayoutBuilder
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.world.block.behavior.BlockBehaviorHolder
import xyz.xenondevs.nova.world.block.state.property.ScopedBlockStateProperty
import xyz.xenondevs.nova.world.block.tileentity.TileEntity

abstract class AbstractNovaBlockBuilder<B : NovaBlock> internal constructor(
    id: ResourceLocation
) : ConfigurableRegistryElementBuilder<B>(NovaRegistries.BLOCK, id) {
    
    internal constructor(addon: Addon, name: String) : this(ResourceLocation(addon, name))
    
    protected var style: Style = Style.empty()
    protected var name: Component = Component.translatable("block.${id.namespace}.${id.path}")
    protected var behaviors = ArrayList<BlockBehaviorHolder>()
    protected val stateProperties = ArrayList<ScopedBlockStateProperty<*>>()
    internal var requestedLayout = BlockModelLayout.DEFAULT
    
    /**
     * Sets the style of the block name.
     */
    fun style(style: Style) {
        this.style = style
    }
    
    /**
     * Sets the style of the block name.
     */
    fun style(color: TextColor) {
        this.style = Style.style(color)
    }
    
    /**
     * Sets the style of the block name.
     */
    fun style(color: TextColor, vararg decorations: TextDecoration) {
        this.style = Style.style(color, *decorations)
    }
    
    /**
     * Sets the style of the block name.
     */
    fun style(vararg decorations: TextDecoration) {
        this.style = Style.style(*decorations)
    }
    
    /**
     * Sets the style of the block name.
     */
    fun style(decoration: TextDecoration) {
        this.style = Style.style(decoration)
    }
    
    /**
     * Sets the name of the block.
     *
     * This function is exclusive with [localizedName].
     */
    fun name(name: Component) {
        this.name = name
    }
    
    /**
     * Sets the localization key of the block.
     *
     * Defaults to `block.<namespace>.<name>`.
     *
     * This function is exclusive with [name].
     */
    fun localizedName(localizedName: String) {
        this.name = Component.translatable(localizedName)
    }
    
    /**
     * Sets the behaviors of this block to [behaviors].
     */
    open fun behaviors(vararg behaviors: BlockBehaviorHolder) {
        this.behaviors += behaviors
    }
    
    /**
     * Adds the [stateProperties] to the properties of this block.
     */
    fun stateProperties(vararg stateProperties: ScopedBlockStateProperty<*>) {
        this.stateProperties += stateProperties
    }
    
    fun models(buildModel: BlockModelLayoutBuilder.() -> Unit) {
        val builder = BlockModelLayoutBuilder()
        builder.buildModel()
        requestedLayout = builder.build()
    }
    
}

class NovaBlockBuilder internal constructor(id: ResourceLocation) : AbstractNovaBlockBuilder<NovaBlock>(id) {
    
    internal constructor(addon: Addon, name: String) : this(ResourceLocation(addon, name))
    
    override fun build() = NovaBlock(
        id,
        name.style(style),
        style,
        behaviors,
        stateProperties,
        configId,
        requestedLayout
    )
    
}

class NovaTileEntityBlockBuilder internal constructor(
    id: ResourceLocation,
    private val tileEntity: TileEntityConstructor
) : AbstractNovaBlockBuilder<NovaTileEntityBlock>(id) {
    
    private var syncTickrate: Int = 20
    private var asyncTickrate: Double = 0.0
    
    internal constructor(
        addon: Addon,
        name: String,
        tileEntity: TileEntityConstructor
    ) : this(ResourceLocation(addon, name), tileEntity)
    
    /**
     * Configures the amount of times [TileEntity.handleTick] is called per second.
     * Accepts values from 0 to 20, with 0 disabling ticking.
     *
     * Defaults to 20.
     */
    fun tickrate(tickrate: Int) {
        require(tickrate in 0..20) { "Sync TPS must be between 0 and 20" }
        this.syncTickrate = tickrate
    }
    
    override fun build() = NovaTileEntityBlock(
        id,
        name.style(style),
        style,
        behaviors,
        tileEntity,
        syncTickrate,
        stateProperties,
        configId,
        requestedLayout
    )
    
}