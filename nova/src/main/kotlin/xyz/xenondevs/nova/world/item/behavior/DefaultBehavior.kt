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
import xyz.xenondevs.nova.util.data.get
import xyz.xenondevs.nova.util.data.logExceptionMessages
import xyz.xenondevs.nova.world.item.NovaItem
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.map
import net.minecraft.util.Unit as MojangUnit

internal class DefaultBehavior(
    id: ResourceLocation,
    name: Provider<Component?>,
    style: Provider<Style>,
    maxStackSize: Provider<Int>,
    attributeModifiers: Provider<ItemAttributeModifiers>,
    defaultCompound: Provider<NamespacedCompound?>
) : ItemBehavior {
    
    override val baseDataComponents = combinedProvider(
        name, style, maxStackSize, attributeModifiers
    ) { name, style, maxStackSize, attributeModifiers ->
        val builder = DataComponentMap.builder()
        if (name != null) {
            builder.set(DataComponents.ITEM_NAME, name.style(style).toNMSComponent())
        } else {
            builder.set(DataComponents.HIDE_TOOLTIP, MojangUnit.INSTANCE)
        }
        
        builder.set(DataComponents.ATTRIBUTE_MODIFIERS, attributeModifiers)
        builder.set(DataComponents.MAX_STACK_SIZE, maxStackSize)
        
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
    
    companion object {
        
        fun create(item: NovaItem, behaviors: List<ItemBehavior>) =
            DefaultBehavior(
                item.id,
                provider(item.name),
                provider(item.style),
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