package xyz.xenondevs.nova.world.item.behavior

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.item.component.ItemLore
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.slf4j.Logger
import org.spongepowered.configurate.ConfigurationNode
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.map
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.config.node
import xyz.xenondevs.nova.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.util.component.adventure.toNMSComponent
import xyz.xenondevs.nova.util.component.adventure.toNmsStyle
import xyz.xenondevs.nova.util.data.get
import xyz.xenondevs.nova.util.data.logExceptionMessages
import xyz.xenondevs.nova.util.item.update
import xyz.xenondevs.nova.util.toResourceLocation
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.TooltipStyle
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.map
import net.minecraft.network.chat.Component as MojangComponent
import net.minecraft.util.Unit as MojangUnit

internal class DefaultBehavior(
    id: ResourceLocation,
    name: Provider<Component?>,
    style: Provider<Style>,
    lore: Provider<List<Component>>,
    tooltipStyle: Provider<TooltipStyle?>,
    maxStackSize: Provider<Int>,
    attributeModifiers: Provider<ItemAttributeModifiers>,
    defaultCompound: Provider<NamespacedCompound?>
) : ItemBehavior {
    
    private val style by style.map { it.toNmsStyle() }
    
    override val baseDataComponents = combinedProvider(
        name, style, lore, tooltipStyle, maxStackSize, attributeModifiers
    ) { name, style, lore, tooltipStyle, maxStackSize, attributeModifiers ->
        val builder = DataComponentMap.builder()
        if (name != null) {
            builder.set(DataComponents.ITEM_NAME, name.style(style).toNMSComponent())
        } else {
            builder.set(DataComponents.HIDE_TOOLTIP, MojangUnit.INSTANCE)
        }
        
        if (lore.isNotEmpty()) {
            builder.set(DataComponents.LORE, ItemLore(lore.map(Component::toNMSComponent)))
        }
        
        if (tooltipStyle != null) {
            builder.set(DataComponents.TOOLTIP_STYLE, tooltipStyle.id.toResourceLocation())
        }
        
        builder.set(DataComponents.ATTRIBUTE_MODIFIERS, attributeModifiers)
        builder.set(DataComponents.MAX_STACK_SIZE, maxStackSize)
        builder.set(DataComponents.ITEM_MODEL, id)
        
        builder.build()
    }
    
    override val defaultPatch = defaultCompound.map { defaultCompound ->
        DataComponentPatch.builder()
            .set(DataComponents.CUSTOM_DATA, CustomData.of(CompoundTag().also { compoundTag ->
                compoundTag.put("nova", CompoundTag().also {
                    it.putString("id", id.toString())
                })
                defaultCompound?.let { compoundTag.putByteArray("nova_cbf", CBF.write(it)) }
            }))
            .build()
    }
    
    override fun modifyClientSideStack(player: Player?, itemStack: ItemStack, data: NamespacedCompound): ItemStack {
        itemStack.unwrap().update(DataComponents.CUSTOM_NAME) {
            val wrappingComponent = MojangComponent.literal("")
            wrappingComponent.setStyle(style)
            wrappingComponent.append(it)
            return@update wrappingComponent
        }
        return itemStack
    }
    
    companion object {
        
        fun create(item: NovaItem, behaviors: List<ItemBehavior>) =
            DefaultBehavior(
                item.id,
                provider(item.name),
                provider(item.style),
                provider(item.lore),
                provider(item.tooltipStyle),
                provider(item.maxStackSize),
                item.config.node("attribute_modifiers").map(::loadConfiguredAttributeModifiers),
                combinedProvider(
                    behaviors.map(ItemBehavior::defaultCompound)
                ) { defaultCompounds ->
                    val compound = NamespacedCompound()
                    for (defaultCompound in defaultCompounds) {
                        compound.putAll(defaultCompound)
                    }
                    
                    compound.takeUnless { it.isEmpty() }
                }
            )
        
        private fun loadConfiguredAttributeModifiers(node: ConfigurationNode): ItemAttributeModifiers {
            if (node.virtual())
                return ItemAttributeModifiers.EMPTY
            
            val builder = ItemAttributeModifiers.builder()
            for ((slotName, attributesNode) in node.childrenMap()) {
                try {
                    val slotGroup = EquipmentSlotGroup.entries.firstOrNull { it.name.equals(slotName as String, true) }
                        ?: throw IllegalArgumentException("Unknown equipment slot group: $slotName")
                    
                    for ((idx, attributeNode) in attributesNode.childrenList().withIndex()) {
                        try {
                            val attribute = attributeNode.node("attribute").get<Attribute>()
                                ?: throw NoSuchElementException("Missing value 'attribute'")
                            val operation = attributeNode.node("operation").get<AttributeModifier.Operation>()
                                ?: throw NoSuchElementException("Missing value 'operation'")
                            val value = attributeNode.node("value").get<Double>()
                                ?: throw NoSuchElementException("Missing value 'value'")
                            
                            builder.add(
                                BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute),
                                AttributeModifier(
                                    ResourceLocation.fromNamespaceAndPath(
                                        "nova",
                                        "configured_attribute_modifier_${slotGroup.name.lowercase()}_$idx"
                                    ),
                                    value,
                                    operation
                                ),
                                slotGroup
                            )
                        } catch (e: Exception) {
                            LOGGER.logExceptionMessages(Logger::warn, "Failed to load attribute modifier for $this, $slotGroup with index $idx", e)
                        }
                    }
                } catch (e: Exception) {
                    LOGGER.logExceptionMessages(Logger::warn, "Failed to load attribute modifier for $this", e)
                }
            }
            
            return builder.build()
        }
    }
    
}