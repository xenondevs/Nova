package xyz.xenondevs.nova.resources.builder.layout.block

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.nova.resources.builder.layout.item.ItemModelDefinitionBuilder
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder
import xyz.xenondevs.nova.world.block.state.model.BackingStateConfigType

internal typealias BlockStateSelector = BlockSelectorScope.() -> BlockState
internal typealias BlockModelSelector = BlockModelSelectorScope.() -> ModelBuilder
internal typealias ItemDefinitionConfigurator = ItemModelDefinitionBuilder<BlockModelSelectorScope>.() -> Unit

internal val DEFAULT_BLOCK_STATE_SELECTOR: BlockStateSelector = { Blocks.BARRIER.defaultBlockState() }
internal val DEFAULT_BLOCK_MODEL_SELECTOR: BlockModelSelector = { defaultModel }

internal sealed interface BlockModelLayout {
    
    class StateBacked(
        val priority: Int,
        val configTypes: List<BackingStateConfigType<*>>,
        val modelSelector: BlockModelSelector
    ) : BlockModelLayout {
        
        override fun toString(): String =
            "StateBacked(priority=$priority, configTypes=$configTypes)"
        
    }
    
    sealed class EntityBacked(
        val stateSelector: BlockStateSelector
    ) : BlockModelLayout
    
    class SimpleEntityBacked(
        stateSelector: BlockStateSelector,
        val modelSelector: BlockModelSelector
    ) : EntityBacked(stateSelector)
    
    class ItemEntityBacked(
        stateSelector: BlockStateSelector,
        val definitionConfigurator: ItemDefinitionConfigurator
    ) : EntityBacked(stateSelector)
    
    class ModelLess(
        val stateSelector: BlockStateSelector
    ) : BlockModelLayout
    
    companion object {
        
        val DEFAULT = SimpleEntityBacked(
            DEFAULT_BLOCK_STATE_SELECTOR,
            DEFAULT_BLOCK_MODEL_SELECTOR
        )
        
    }
    
}