@file:Suppress("INAPPLICABLE_JVM_NAME")

package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.minecraft.resources.RegistryOps
import net.minecraft.world.item.Item
import org.bukkit.block.BlockType
import org.bukkit.inventory.ItemType
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.provider.uninitializedProvider
import xyz.xenondevs.nova.config.CONFIGS
import xyz.xenondevs.nova.registry.entries.ItemTypeEntries
import xyz.xenondevs.nova.resources.builder.layout.block.BlockModelLayout
import xyz.xenondevs.nova.resources.builder.layout.block.BlockModelSelectorScope
import xyz.xenondevs.nova.resources.builder.layout.item.ItemModelDefinitionBuilder
import xyz.xenondevs.nova.resources.builder.layout.item.ItemModelSelectorScope
import xyz.xenondevs.nova.resources.builder.task.BlockModelTask
import xyz.xenondevs.nova.resources.builder.task.ItemModelContent
import xyz.xenondevs.nova.resources.builder.task.ModelContent
import xyz.xenondevs.nova.util.component.adventure.withoutPreFormatting
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.TooltipStyle
import xyz.xenondevs.nova.world.item.behavior.BlockItemBehavior
import xyz.xenondevs.nova.world.item.behavior.DefaultBehavior
import xyz.xenondevs.nova.world.item.behavior.ItemBehavior
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorFactory
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorHolder

internal class NovaItemBuilderImpl(
    override val entry: RegistryEntry.Paper<ItemType>
) : NovaItemBuilder, RegistryElementBuilder.RerunnableVanilla<Item> {
    
    private val key: Key = entry.key
    
    private val _configId = uninitializedProvider<String>()
    private val _style = uninitializedProvider<Style>()
    private val _name = uninitializedProvider<Component?>()
    private val _lore = uninitializedProvider<List<Component>>()
    private val _behaviorHolders = uninitializedProvider<List<ItemBehaviorHolder>>()
    private val _maxStackSize = uninitializedProvider<Int>()
    private val _craftingRemainingItem = uninitializedProvider<RegistryEntry.Paper<ItemType>>()
    private val _isHidden = uninitializedProvider<Boolean>()
    private val _block = uninitializedProvider<RegistryEntry.Paper<BlockType>?>()
    private val _tooltipStyle = uninitializedProvider<RegistryEntry.Nova<TooltipStyle>?>()
    
    private var configId: String by _configId
    private var style: Style by _style
    private var name: Component? by _name
    private var lore: List<Component> by _lore
    private var behaviorHolders: List<ItemBehaviorHolder> by _behaviorHolders
    private var maxStackSize: Int by _maxStackSize
    private var craftingRemainingItem: RegistryEntry.Paper<ItemType> by _craftingRemainingItem
    private var isHidden: Boolean by _isHidden
    private var block: RegistryEntry.Paper<BlockType>? by _block
    private var tooltipStyle: RegistryEntry.Nova<TooltipStyle>? by _tooltipStyle
    
    private var configureDefinition: ItemModelDefinitionBuilder<ItemModelSelectorScope>.() -> Unit =
        ItemModelDefinitionBuilder.DEFAULT_CONFIGURE_ITEM_MODEL_SELECTOR
    
    override fun reset() {
        _configId.set(key.asString())
        _style.set(Style.empty())
        _name.set(Component.translatable("item.${key.namespace()}.${key.value()}"))
        _lore.set(emptyList())
        _behaviorHolders.set(emptyList())
        _maxStackSize.set(64)
        _craftingRemainingItem.set { ItemTypeEntries.AIR }
        _isHidden.set(false)
        _block.set(null)
        _tooltipStyle.set(null)
    }
    
    override fun block(block: RegistryEntry.Paper<BlockType>) {
        blockItems.getOrPut(block) { mutableProvider(null) }.set(entry)
        this.block = block
        // note that this does not use NovaBlock.name as that would require making the name a Provider
        name(Component.translatable("block.${block.key.namespace()}.${block.key.value()}"))
        modelDefinition {
            val [layout, blockStates] = BlockModelTask.requests[block]!!
            
            val modelContent = resourcePackBuilder.getBuildData<ModelContent>()
            val scope = BlockModelSelectorScope(blockStates[0], resourcePackBuilder, modelContent)
            
            model = when (layout) {
                is BlockModelLayout.StateBacked -> buildModel { layout.modelSelector(scope) }
                is BlockModelLayout.SimpleEntityBacked -> buildModel { layout.modelSelector(scope) }
                is BlockModelLayout.ItemEntityBacked -> {
                    val builder = ItemModelDefinitionBuilder(resourcePackBuilder) { modelSelector ->
                        val model = modelSelector(scope).buildScaled(modelContent).model
                        val id = modelContent.getOrPutGenerated(model)
                        modelContent.rememberUsage(id)
                        id
                    }
                    layout.definitionConfigurator(builder)
                    builder.build().model
                }
                
                else -> buildModel { defaultModel }
            }
        }
    }
    
    override fun config(name: String) {
        this.configId = key.namespace() + ":" + name
    }
    
    override fun rawConfig(id: String) {
        this.configId = id
    }
    
    override fun style(style: Style) {
        this.style = style
    }
    
    @JvmName("nameNullable")
    override fun name(name: Component?) {
        this.name = name
    }
    
    override fun lore(vararg lines: Component) {
        this.lore += lines.map { it.withoutPreFormatting() }
    }
    
    override fun maxStackSize(maxStackSize: Int) {
        if (maxStackSize > 99)
            throw IllegalArgumentException("Max stack size cannot exceed 99")
        
        this.maxStackSize = maxStackSize
    }
    
    override fun behaviors(vararg itemBehaviors: ItemBehaviorHolder) {
        this.behaviorHolders = itemBehaviors.toMutableList()
    }
    
    @JvmName("craftingRemainingItemItemType")
    override fun craftingRemainingItem(item: RegistryEntry.Paper<ItemType>) {
        craftingRemainingItem = item
    }
    
    override fun hidden(hidden: Boolean) {
        this.isHidden = hidden
    }
    
    override fun tooltipStyle(tooltipStyle: RegistryEntry.Nova<TooltipStyle>) {
        this.tooltipStyle = tooltipStyle
    }
    
    override fun modelDefinition(itemModelDefinition: ItemModelDefinitionBuilder<ItemModelSelectorScope>.() -> Unit) {
        this.configureDefinition = itemModelDefinition
    }
    
    override fun prepareBuild() {
        ItemModelContent.request(entry, configureDefinition)
    }
    
    override fun build(lookup: RegistryOps.RegistryInfoLookup): Item {
        val config = _configId.map(CONFIGS::get)
        
        val behaviors = combinedProvider(
            _name, _style, _lore, _tooltipStyle, _maxStackSize, _behaviorHolders, _block, config
        ) { name, style, lore, tooltipStyle, maxStackSize, behaviorHolders, block, config ->
            buildList {
                this += DefaultBehavior(key, name, style, lore, tooltipStyle, maxStackSize)
                if (block != null)
                    this += BlockItemBehavior(block)
                
                for (holder in behaviorHolders) {
                    this += when (holder) {
                        is ItemBehavior -> holder
                        is ItemBehaviorFactory<*> -> holder.create(entry, config)
                    }
                }
            }
        }
        
        return NovaItem(entry, behaviors, _craftingRemainingItem, _isHidden, _block, config)
    }
    
    companion object {
        
        val blockItems = HashMap<RegistryEntry.Paper<BlockType>, MutableProvider<RegistryEntry.Paper<ItemType>?>>()
        
    }
    
}
