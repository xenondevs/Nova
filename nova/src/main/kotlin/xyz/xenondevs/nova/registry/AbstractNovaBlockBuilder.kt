package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import xyz.xenondevs.commons.collections.flatMap
import xyz.xenondevs.nova.resources.builder.layout.block.BackingStateCategory
import xyz.xenondevs.nova.resources.builder.layout.block.BlockModelLayout
import xyz.xenondevs.nova.resources.builder.layout.block.BlockModelSelector
import xyz.xenondevs.nova.resources.builder.layout.block.BlockStateSelector
import xyz.xenondevs.nova.resources.builder.layout.block.ItemDefinitionConfigurator
import xyz.xenondevs.nova.resources.builder.task.BlockModelTask
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.behavior.BlockBehaviorHolder
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.property.ScopedBlockStateProperty

internal abstract class AbstractNovaBlockBuilder<T : NovaBlock>(
    protected val entry: RegistryEntry.Nova<NovaBlock>
) : NovaBlockBuilder, RegistryElementBuilder.Nova<NovaBlock> {
    
    protected val key: Key = entry.key
    protected var configId: String = key.toString()
    protected var style: Style = Style.empty()
    protected var name: Component = Component.translatable("block.${key.namespace()}.${key.value()}")
    protected var behaviors = ArrayList<BlockBehaviorHolder>()
    protected val stateProperties = ArrayList<ScopedBlockStateProperty<*>>()
    internal var layout: BlockModelLayout = BlockModelLayout.DEFAULT
    
    protected lateinit var blockStates: List<NovaBlockState>
    
    override fun config(name: String) {
        this.configId = key.namespace() + ":" + name
    }
    
    override fun rawConfig(id: String) {
        this.configId = id
    }
    
    override fun style(style: Style) {
        this.style = style
    }
    
    override fun name(name: Component) {
        this.name = name
    }
    
    override fun behaviors(vararg behaviors: BlockBehaviorHolder) {
        this.behaviors += behaviors
    }
    
    override fun stateProperties(vararg stateProperties: ScopedBlockStateProperty<*>) {
        this.stateProperties += stateProperties
    }
    
    override fun stateBacked(
        priority: Int,
        category: BackingStateCategory,
        vararg categories: BackingStateCategory,
        modelSelector: BlockModelSelector
    ) {
        layout = BlockModelLayout.StateBacked(
            priority,
            listOf(category, *categories).flatMap { it.backingStateConfigTypes },
            modelSelector
        )
    }
    
    override fun entityBacked(
        stateSelector: BlockStateSelector,
        modelSelector: BlockModelSelector
    ) {
        layout = BlockModelLayout.SimpleEntityBacked(stateSelector, modelSelector)
    }
    
    override fun entityItemBacked(
        stateSelector: BlockStateSelector,
        itemSelector: ItemDefinitionConfigurator
    ) {
        layout = BlockModelLayout.ItemEntityBacked(stateSelector, itemSelector)
    }
    
    override fun modelLess(stateSelector: BlockStateSelector) {
        layout = BlockModelLayout.ModelLess(stateSelector)
    }
    
    override fun prepareBuild() {
        this.blockStates = NovaBlockState.createBlockStates(entry, stateProperties)
        BlockModelTask.request(entry, layout, blockStates)
    }
    
}