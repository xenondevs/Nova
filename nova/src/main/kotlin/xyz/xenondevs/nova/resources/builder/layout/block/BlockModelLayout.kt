package xyz.xenondevs.nova.resources.builder.layout.block

import org.bukkit.block.BlockType
import org.bukkit.block.data.BlockData
import xyz.xenondevs.nova.resources.builder.layout.item.ItemModelDefinitionBuilder
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder
import xyz.xenondevs.nova.world.block.state.model.BackingStateConfigType

internal typealias BlockStateSelector = BlockSelectorScope.() -> BlockData
internal typealias BlockModelSelector = BlockModelSelectorScope.() -> ModelBuilder
internal typealias ItemDefinitionConfigurator = ItemModelDefinitionBuilder<BlockModelSelectorScope>.() -> Unit

internal val DEFAULT_BLOCK_STATE_SELECTOR: BlockStateSelector = { BlockType.BARRIER.createBlockData() }
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
    
    sealed interface EntityBacked : BlockModelLayout {
        val stateSelector: BlockStateSelector
    }
    
    class SimpleEntityBacked(
        override val stateSelector: BlockStateSelector,
        val modelSelector: BlockModelSelector
    ) : EntityBacked
    
    class ItemEntityBacked(
        override val stateSelector: BlockStateSelector,
        val definitionConfigurator: ItemDefinitionConfigurator
    ) : EntityBacked
    
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