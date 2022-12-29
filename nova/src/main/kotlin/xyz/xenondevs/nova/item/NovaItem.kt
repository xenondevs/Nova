@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.item

import de.studiocode.invui.item.builder.ItemBuilder
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.TranslatableComponent
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.MobType
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.enchantment.EnchantmentHelper
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.provider.combinedLazyProvider
import xyz.xenondevs.nova.data.provider.flatten
import xyz.xenondevs.nova.data.provider.map
import xyz.xenondevs.nova.data.resources.builder.content.material.info.VanillaMaterialTypes
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.item.behavior.ItemBehaviorHolder
import xyz.xenondevs.nova.item.behavior.Tool
import xyz.xenondevs.nova.item.vanilla.AttributeModifier
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.util.bukkitCopy
import xyz.xenondevs.nova.util.data.appendLocalized
import xyz.xenondevs.nova.util.data.getConfigurationSectionList
import xyz.xenondevs.nova.util.data.getDoubleOrNull
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.data.logExceptionMessages
import xyz.xenondevs.nova.util.data.withoutPreFormatting
import xyz.xenondevs.nova.util.enumMapOf
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.util.takeUnlessEmpty
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.logging.Level
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses
import net.minecraft.world.item.ItemStack as MojangStack

private val ATTRIBUTE_DECIMAL_FORMAT = DecimalFormat("#.##")
    .apply { decimalFormatSymbols = DecimalFormatSymbols.getInstance(Locale.ROOT) }

/**
 * Handles actions performed on [ItemStack]s of a [ItemNovaMaterial]
 */
class NovaItem internal constructor(holders: List<ItemBehaviorHolder<*>>) {
    
    val behaviors: List<ItemBehavior> by lazy { holders.map { it.get(material) } }
    private lateinit var material: ItemNovaMaterial
    private lateinit var name: Array<BaseComponent>
    
    internal val vanillaMaterialProvider = combinedLazyProvider { behaviors.map(ItemBehavior::vanillaMaterialProperties) }
        .flatten()
        .map { VanillaMaterialTypes.getMaterial(it.toHashSet()) }
    internal val configuredAttributeModifiersProvider by lazy { configReloadable(::loadConfiguredAttributeModifiers) }
    internal val attributeModifiersProvider = combinedLazyProvider { behaviors.map(ItemBehavior::attributeModifiers) + configuredAttributeModifiersProvider }
        .flatten()
        .map { modifiers ->
            val map = enumMapOf<EquipmentSlot, ArrayList<AttributeModifier>>()
            modifiers.forEach { modifier -> modifier.slots.forEach { slot -> map.getOrPut(slot, ::ArrayList) += modifier } }
            return@map map
        }
    
    internal val vanillaMaterial: Material by vanillaMaterialProvider
    internal val attributeModifiers: Map<EquipmentSlot, List<AttributeModifier>> by attributeModifiersProvider
    
    internal constructor(vararg holders: ItemBehaviorHolder<*>) : this(holders.toList())
    
    @Suppress("UNCHECKED_CAST")
    fun <T : ItemBehavior> getBehavior(type: KClass<T>): T? {
        return behaviors.firstOrNull { type == it::class || type in it::class.superclasses } as T?
    }
    
    fun hasBehavior(type: KClass<out ItemBehavior>): Boolean {
        return behaviors.any { it::class == type }
    }
    
    internal fun setMaterial(material: ItemNovaMaterial) {
        if (::material.isInitialized)
            throw IllegalStateException("NovaItems cannot be used for multiple materials")
        
        this.material = material
        this.name = TranslatableComponent(material.localizedName).withoutPreFormatting()
    }
    
    internal fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
        var builder = itemBuilder
        behaviors.forEach { builder = it.modifyItemBuilder(builder) }
        return builder
    }
    
    internal fun getPacketItemData(player: Player?, itemStack: MojangStack): PacketItemData {
        val itemData = PacketItemData(itemStack.tag!!)
        val bukkitStack = itemStack.bukkitCopy
        
        behaviors.forEach { it.updatePacketItemData(bukkitStack, itemData) }
        itemData.addLore(generateAttributeModifiersTooltip(player, itemStack))
        if (itemData.name == null) itemData.name = this.name
        
        return itemData
    }
    
    private fun loadConfiguredAttributeModifiers(): List<AttributeModifier> {
        val section = NovaConfig.getOrNull(material)
            ?.getConfigurationSection("attribute_modifiers")
        ?: return emptyList()
        
        val modifiers = ArrayList<AttributeModifier>()
        
        section.getKeys(false)
            .forEach { key ->
                try {
                    val slot = EquipmentSlot.values().firstOrNull { it.name == key.uppercase() }
                        ?: throw IllegalArgumentException("Unknown equipment slot: $key")
                    val attributeSections = section.getConfigurationSectionList(key).takeUnlessEmpty()
                        ?: throw IllegalArgumentException("No attribute modifiers defined for slot $key")
    
                    attributeSections.forEachIndexed { idx, attributeSection ->
                        try {
                            val attributeStr = attributeSection.getString("attribute")
                                ?: throw IllegalArgumentException("Missing value 'attribute'")
                            val operationStr = attributeSection.getString("operation")
                                ?: throw IllegalArgumentException("Missing value 'operation'")
                            val value = attributeSection.getDoubleOrNull("value")
                                ?: throw IllegalArgumentException("Missing value 'value'")
                            val hidden = attributeSection.getBoolean("hidden", false)
                            
                            val attribute = BuiltInRegistries.ATTRIBUTE.get(ResourceLocation(attributeStr))
                                ?: throw IllegalArgumentException("Unknown attribute: $attributeStr")
                            val operation = Operation.values().firstOrNull { it.name == operationStr.uppercase() }
                                ?: throw IllegalArgumentException("Unknown operation: $operationStr")
            
                            modifiers += AttributeModifier(
                                "Nova Configured Attribute Modifier ($slot, $idx)",
                                attribute,
                                operation,
                                value,
                                !hidden,
                                slot
                            )
                        } catch (e: Exception) {
                            LOGGER.logExceptionMessages(Level.WARNING, "Failed to load attribute modifier for $material, $slot with index $idx", e)
                        }
                    }
                } catch(e: Exception) {
                    LOGGER.logExceptionMessages(Level.WARNING, "Failed to load attribute modifier for $material", e)
                }
            }
        
        return modifiers
    }
    
    private fun generateAttributeModifiersTooltip(player: Player?, itemStack: MojangStack): List<Array<BaseComponent>> {
        val lore = ArrayList<Array<BaseComponent>>()
        
        attributeModifiers.forEach { (slot, modifiers) ->
            if (modifiers.none { it.showInLore && it.value != 0.0 })
                return@forEach
            
            lore += arrayOf(TextComponent(" "))
            lore += arrayOf(localized(ChatColor.GRAY, "item.modifiers.${slot.name.lowercase()}"))
            
            modifiers.asSequence()
                .filter { it.showInLore && it.value != 0.0 }
                .forEach { modifier ->
                    var value = modifier.value
                    var isBaseModifier = false
                    
                    if (player != null) {
                        val serverPlayer = player.serverPlayer
                        when (modifier.uuid) {
                            Tool.BASE_ATTACK_DAMAGE_UUID -> {
                                value += serverPlayer.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)
                                value += EnchantmentHelper.getDamageBonus(itemStack, MobType.UNDEFINED)
                                isBaseModifier = true
                            }
                            
                            Tool.BASE_ATTACK_SPEED_UUID -> {
                                value += serverPlayer.getAttributeBaseValue(Attributes.ATTACK_SPEED)
                                isBaseModifier = true
                            }
                        }
                    }
                    
                    var displayedValue = if (modifier.operation == Operation.ADDITION) {
                        if (modifier.attribute == Attributes.KNOCKBACK_RESISTANCE) {
                            value * 10.0 // no idea why, but this is how it is done in vanilla
                        } else value
                    } else value * 100.0
                    
                    fun appendModifier(type: String, color: ChatColor) {
                        lore += ComponentBuilder(if (isBaseModifier) " " else "")
                            .appendLocalized(
                                "attribute.modifier.$type.${modifier.operation.ordinal}",
                                ATTRIBUTE_DECIMAL_FORMAT.format(displayedValue),
                                TranslatableComponent(modifier.attribute.descriptionId)
                            )
                            .color(color)
                            .create()
                    }
                    
                    if (isBaseModifier) {
                        appendModifier("equals", ChatColor.DARK_GREEN)
                    } else if (value > 0.0) {
                        appendModifier("plus", ChatColor.BLUE)
                    } else if (value < 0.0) {
                        displayedValue *= -1
                        appendModifier("take", ChatColor.RED)
                    }
                }
        }
        
        return lore
    }
    
}