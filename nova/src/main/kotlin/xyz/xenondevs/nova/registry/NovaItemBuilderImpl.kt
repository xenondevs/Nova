@file:Suppress("INAPPLICABLE_JVM_NAME")

package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import xyz.xenondevs.commons.provider.NULL_PROVIDER
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.config.CONFIGS
import xyz.xenondevs.nova.resources.builder.layout.block.BlockModelLayout
import xyz.xenondevs.nova.resources.builder.layout.block.BlockModelSelectorScope
import xyz.xenondevs.nova.resources.builder.layout.item.ItemModelDefinitionBuilder
import xyz.xenondevs.nova.resources.builder.layout.item.ItemModelSelectorScope
import xyz.xenondevs.nova.resources.builder.task.BlockModelTask
import xyz.xenondevs.nova.resources.builder.task.ItemModelContent
import xyz.xenondevs.nova.resources.builder.task.ModelContent
import xyz.xenondevs.nova.util.component.adventure.withoutPreFormatting
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.TooltipStyle
import xyz.xenondevs.nova.world.item.behavior.BlockItemBehavior
import xyz.xenondevs.nova.world.item.behavior.DefaultBehavior
import xyz.xenondevs.nova.world.item.behavior.ItemBehavior
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorFactory
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorHolder
import xyz.xenondevs.nova.world.item.createItemStack

internal class NovaItemBuilderImpl(
    override val entry: RegistryEntry.Nova<NovaItem>
) : NovaItemBuilder, RegistryElementBuilder.Nova<NovaItem> {
    
    private val key: Key = entry.key
    private var configId: String = key.toString()
    private var style: Style = Style.empty()
    private var name: Component? = Component.translatable("item.${key.namespace()}.${key.value()}")
    private val lore = ArrayList<Component>()
    private var behaviorHolders: MutableList<ItemBehaviorHolder> = ArrayList()
    private var maxStackSize = 64
    private var craftingRemainingItem: Lazy<ItemStack> = lazy { ItemStack.empty() }
    private var isHidden = false
    private var block: RegistryEntry.Nova<NovaBlock>? = null
    private var tooltipStyle: RegistryEntry.Nova<TooltipStyle>? = null
    private var configureDefinition: ItemModelDefinitionBuilder<ItemModelSelectorScope>.() -> Unit = ItemModelDefinitionBuilder.DEFAULT_CONFIGURE_ITEM_MODEL_SELECTOR
    
    override fun block(block: RegistryEntry.Nova<NovaBlock>) {
        this.block = block
        // note that this does not use NovaBlock.name as that would require making the name a Provider
        name(Component.translatable("block.${block.key.namespace()}.${block.key.value()}"))
        modelDefinition {
            val (layout, blockStates) = BlockModelTask.requests[block]!!
            
            val modelContent = resourcePackBuilder.getBuildData<ModelContent>()
            val scope = BlockModelSelectorScope(blockStates[0], resourcePackBuilder, modelContent)
            
            model = when (layout) {
                is BlockModelLayout.StateBacked -> buildModel { layout.modelSelector(scope) }
                is BlockModelLayout.SimpleEntityBacked -> buildModel { layout.modelSelector(scope) }
                is BlockModelLayout.ItemEntityBacked -> {
                    val builder = ItemModelDefinitionBuilder(resourcePackBuilder) { modelSelector ->
                        val (model, _) = modelSelector(scope).buildScaled(modelContent)
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
    
    @JvmName("craftingRemainingItemNullable")
    override fun craftingRemainingItem(item: RegistryEntry.Nova<NovaItem>) {
        craftingRemainingItem = lazy { item.createItemStack() }
    }
    
    @JvmName("craftingRemainingItemItemType")
    override fun craftingRemainingItem(item: RegistryEntry.Paper<ItemType>) {
        craftingRemainingItem = lazy { item.createItemStack() }
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
    
    override fun build(): NovaItem {
        val config = CONFIGS[configId]
        
        val behaviors = buildList {
            this += DefaultBehavior(
                key,
                provider(name),
                provider(style),
                provider(lore),
                tooltipStyle ?: NULL_PROVIDER,
                provider(maxStackSize),
                config
            )
            block?.let { this += BlockItemBehavior(it) }
            
            for (holder in behaviorHolders) {
                this += when (holder) {
                    is ItemBehavior -> holder
                    is ItemBehaviorFactory<*> -> holder.create(entry, config)
                }
            }
        }
        
        return NovaItem(
            entry,
            name?.style(style),
            lore,
            style,
            behaviors,
            maxStackSize,
            craftingRemainingItem,
            isHidden,
            block,
            CONFIGS[configId],
            tooltipStyle
        )
    }
    
    companion object {
        
        val blockItems = HashMap<RegistryEntry.Nova<NovaBlock>, RegistryEntry.Nova<NovaItem>>()
        
        fun fromBlock(
            item: RegistryEntry.Nova<NovaItem>,
            block: RegistryEntry.Nova<NovaBlock>
        ): NovaItemBuilderImpl {
            blockItems[block] = item
            return NovaItemBuilderImpl(item).apply { block(block) }
        }
        
    }
    
}