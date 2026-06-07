package xyz.xenondevs.nova.world.item.behavior

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers.itemAttributes
import io.papermc.paper.datacomponent.item.attribute.AttributeModifierDisplay
import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.mapNonNull
import xyz.xenondevs.commons.provider.orElseBy
import xyz.xenondevs.nova.config.ConfigProvider
import xyz.xenondevs.nova.config.optionalEntry
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.serialization.kotlinx.AttributeModifierDisplaySerializer
import xyz.xenondevs.nova.serialization.kotlinx.AttributeModifierOperationSerializer
import xyz.xenondevs.nova.serialization.kotlinx.AttributeSerializer
import xyz.xenondevs.nova.serialization.kotlinx.KeySerializer
import xyz.xenondevs.nova.util.toNamespacedKey
import xyz.xenondevs.nova.world.item.buildDataComponentMapProvider

@Serializable
private class AttributesSurrogate(
    @Serializable(KeySerializer::class)
    val id: Key? = null,
    @Serializable(AttributeSerializer::class)
    val attribute: Attribute,
    @Serializable(AttributeModifierOperationSerializer::class)
    val operation: AttributeModifier.Operation,
    val value: Double,
    @Serializable(AttributeModifierDisplaySerializer::class)
    val display: AttributeModifierDisplay = AttributeModifierDisplay.reset()
)

private fun loadConfiguredAttributeModifiers(key: Key, config: ConfigProvider): Provider<ItemAttributeModifiers?> =
    config.optionalEntry<Map<EquipmentSlotGroup, List<AttributesSurrogate>>>("attribute_modifiers").mapNonNull {
        val builder = itemAttributes()
        for ([slotGroup, attributes] in it) {
            for (attribute in attributes) {
                val id = attribute.id
                    ?: Key.key(key.namespace(), "${key.value()}_${slotGroup.toString().lowercase()}")
                
                builder.addModifier(
                    attribute.attribute,
                    AttributeModifier(
                        id.toNamespacedKey(),
                        attribute.value,
                        attribute.operation,
                        slotGroup
                    ),
                    attribute.display
                )
            }
        }
        return@mapNonNull builder.build()
    }

/**
 * Creates a factory for [AttributeModifiers] behaviors using the given values, if not specified otherwise in the item's config.
 * 
 * @param modifiers The attribute modifiers of the item.
 * Defaults to an empty attribute modifiers list.
 * Used when `attribute_modifiers` is not specified in the item's config, structured like this:
 * ```
 * attribute_modifiers:
 *   <equipment_slot>: # any / mainhand / offhand / hand / feet / legs / chest / head / armor / body
 *   - attribute: <attribute> # e.g. attack_damage
 *     operation: <operation> # add_value / add_multiplied_base / add_multiplied_total
 *     value: <value> # e.g. 5.0
 * ```
 */
@Suppress("FunctionName")
fun AttributeModifiers(
    modifiers: Map<RegistryEntry.Paper<Attribute>, AttributeModifier> = emptyMap()
) = ItemBehaviorFactory { entry, cfg ->
    val default = modifiers.entries.map { [attributeEntry, modifier] -> attributeEntry.map { it to modifier } }
        .let(::combinedProvider)
        .map { attributes ->
            itemAttributes().apply {
                for ([attribute, modifier] in attributes) {
                    addModifier(attribute, modifier)
                }
            }.build()
        }
    AttributeModifiers(loadConfiguredAttributeModifiers(entry.key, cfg).orElseBy(default))
}

/**
 * Gives an item [modifiers].
 */
class AttributeModifiers(modifiers: Provider<ItemAttributeModifiers>) : ItemBehavior {
    
    /**
     * The attribute modifiers of this item.
     */
    val modifiers by modifiers
    
    override val baseDataComponents = buildDataComponentMapProvider {
        this[DataComponentTypes.ATTRIBUTE_MODIFIERS] = modifiers
    }
    
    override fun toString(itemStack: ItemStack): String {
        return "AttributeModifiers(modifiers=$modifiers)"
    }
    
}