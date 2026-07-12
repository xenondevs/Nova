package xyz.xenondevs.nova.resources.builder.layout.block

import org.bukkit.block.BlockType
import org.bukkit.block.data.BlockData
import xyz.xenondevs.nova.resources.builder.layout.item.ItemModelDefinitionBuilder
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder
import xyz.xenondevs.nova.world.block.state.model.BackingStateConfigType

internal val DEFAULT_BLOCK_STATE_SELECTOR: BlockSelectorScope.() -> BlockData = { BlockType.BARRIER.createBlockData() }
internal val DEFAULT_BLOCK_MODEL_SELECTOR: BlockModelSelectorScope.() -> ModelBuilder = { defaultModel }

internal sealed interface BlockModelLayout {
    
    class StateBacked(
        val priority: Int,
        val configTypes: List<BackingStateConfigType<*>>,
        val modelSelector: BlockModelSelectorScope.() -> ModelBuilder
    ) : BlockModelLayout {
        
        override fun toString(): String =
            "StateBacked(priority=$priority, configTypes=$configTypes)"
        
    }
    
    sealed interface EntityBacked : BlockModelLayout {
        val stateSelector: BlockSelectorScope.() -> BlockData
    }
    
    class SimpleEntityBacked(
        override val stateSelector: BlockSelectorScope.() -> BlockData,
        val modelSelector: BlockModelSelectorScope.() -> ModelBuilder
    ) : EntityBacked
    
    class ItemEntityBacked(
        override val stateSelector: BlockSelectorScope.() -> BlockData,
        val definitionConfigurator: ItemModelDefinitionBuilder<BlockModelSelectorScope>.() -> Unit
    ) : EntityBacked
    
    class ModelLess(
        val stateSelector: BlockSelectorScope.() -> BlockData
    ) : BlockModelLayout
    
    companion object {
        
        val DEFAULT = SimpleEntityBacked(
            DEFAULT_BLOCK_STATE_SELECTOR,
            DEFAULT_BLOCK_MODEL_SELECTOR
        )
        
    }
    
}
