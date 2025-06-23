package xyz.xenondevs.nova.world.item.behavior

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers.itemAttributes
import io.papermc.paper.datacomponent.item.ItemEnchantments.itemEnchantments
import io.papermc.paper.datacomponent.item.ItemLore.lore
import io.papermc.paper.datacomponent.item.TooltipDisplay.tooltipDisplay
import io.papermc.paper.datacomponent.item.attribute.AttributeModifierDisplay
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.minecraft.core.component.DataComponents
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import org.slf4j.Logger
import org.spongepowered.configurate.ConfigurationNode
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.config.node
import xyz.xenondevs.nova.util.component.adventure.toNmsStyle
import xyz.xenondevs.nova.util.data.get
import xyz.xenondevs.nova.util.data.logExceptionMessages
import xyz.xenondevs.nova.util.item.update
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.item.DataComponentMap
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.TooltipStyle
import xyz.xenondevs.nova.world.item.buildDataComponentMap
import net.minecraft.network.chat.Component as MojangComponent

internal class DefaultBehavior(
    id: Key,
    name: Provider<Component?>,
    style: Provider<Style>,
    lore: Provider<List<Component>>,
    tooltipStyle: Provider<TooltipStyle?>,
    maxStackSize: Provider<Int>,
    attributeModifiers: Provider<ItemAttributeModifiers>,
) : ItemBehavior {
    
    private val style by style.map { it.toNmsStyle() }
    
    override val baseDataComponents: Provider<DataComponentMap> = combinedProvider(
        name, style, lore, tooltipStyle, maxStackSize, attributeModifiers
    ) { name, style, lore, tooltipStyle, maxStackSize, attributeModifiers ->
        buildDataComponentMap {
            if (name != null) {
                this[DataComponentTypes.ITEM_NAME] = name.style(style)
            } else {
                this[DataComponentTypes.TOOLTIP_DISPLAY] = tooltipDisplay().hideTooltip(true).build()
            }
            
            if (lore.isNotEmpty()) {
                this[DataComponentTypes.LORE] = lore(lore)
            }
            
            if (tooltipStyle != null) {
                this[DataComponentTypes.TOOLTIP_STYLE] = tooltipStyle.id
            }
            
            this[DataComponentTypes.ATTRIBUTE_MODIFIERS] = attributeModifiers
            this[DataComponentTypes.MAX_STACK_SIZE] = maxStackSize
            this[DataComponentTypes.ITEM_MODEL] = id
            
            // default empty values
            this[DataComponentTypes.ENCHANTMENTS] = itemEnchantments().build()
            this[DataComponentTypes.REPAIR_COST] = 0
            this[DataComponentTypes.RARITY] = ItemRarity.COMMON
        }
    }
    
    override fun modifyClientSideStack(player: Player?, server: ItemStack, client: ItemStack): ItemStack {
        client.unwrap().update(DataComponents.CUSTOM_NAME) {
            val wrappingComponent = MojangComponent.literal("")
            wrappingComponent.style = style
            wrappingComponent.append(it)
            return@update wrappingComponent
        }
        return client
    }
    
    companion object : ItemBehaviorFactory<DefaultBehavior> {
        
        override fun create(item: NovaItem) =
            DefaultBehavior(
                item.id,
                provider(item.name),
                provider(item.style),
                provider(item.lore),
                provider(item.tooltipStyle),
                provider(item.maxStackSize),
                item.config.node("attribute_modifiers").map { loadConfiguredAttributeModifiers(item, it) }
            )
        
        private fun loadConfiguredAttributeModifiers(item: NovaItem, node: ConfigurationNode): ItemAttributeModifiers {
            if (node.virtual())
                return itemAttributes().build()
            
            val builder = itemAttributes()
            for ((slotName, attributesNode) in node.childrenMap()) {
                try {
                    val slotGroup = EquipmentSlotGroup.getByName(slotName as String)
                        ?: throw IllegalArgumentException("Unknown equipment slot group: $slotName")
                    
                    for ((idx, attributeNode) in attributesNode.childrenList().withIndex()) {
                        try {
                            val id = attributeNode.node("id").get<String>()
                                ?: "${item.id.namespace()}:${item.id.value()}_${slotGroup.toString().lowercase()}_$idx"
                            val attribute = attributeNode.node("attribute").get<Attribute>()
                                ?: throw NoSuchElementException("Missing value 'attribute'")
                            val operation = attributeNode.node("operation").get<AttributeModifier.Operation>()
                                ?: throw NoSuchElementException("Missing value 'operation'")
                            val value = attributeNode.node("value").get<Double>()
                                ?: throw NoSuchElementException("Missing value 'value'")
                            val display = attributeNode.node("display").get<AttributeModifierDisplay>()
                                ?: AttributeModifierDisplay.reset()
                            
                            println(display)
                            
                            builder.addModifier(
                                attribute,
                                AttributeModifier(
                                    NamespacedKey.fromString(id) ?: throw IllegalArgumentException("Illegal id: $id"),
                                    value,
                                    operation,
                                    slotGroup
                                ),
                                display
                            )
                        } catch (e: Exception) {
                            LOGGER.logExceptionMessages(Logger::warn, "Failed to load attribute modifier for $item, $slotGroup with index $idx", e)
                        }
                    }
                } catch (e: Exception) {
                    LOGGER.logExceptionMessages(Logger::warn, "Failed to load attribute modifier for $item", e)
                }
            }
            
            return builder.build()
        }
    }
    
}