@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.item

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.MobType
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.enchantment.EnchantmentHelper
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.commons.collections.takeUnlessEmpty
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.nmsutils.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.Reloadable
import xyz.xenondevs.nova.data.resources.builder.content.material.info.VanillaMaterialTypes
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.item.behavior.ItemBehaviorHolder
import xyz.xenondevs.nova.item.behavior.Tool
import xyz.xenondevs.nova.item.vanilla.AttributeModifier
import xyz.xenondevs.nova.item.vanilla.HideableFlag
import xyz.xenondevs.nova.material.NovaItem
import xyz.xenondevs.nova.player.equipment.ArmorEquipEvent
import xyz.xenondevs.nova.util.data.getConfigurationSectionList
import xyz.xenondevs.nova.util.data.getDoubleOrNull
import xyz.xenondevs.nova.util.data.logExceptionMessages
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.item.novaCompound
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.world.block.event.BlockBreakActionEvent
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.logging.Level
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf
import net.minecraft.world.item.ItemStack as MojangStack

private val ATTRIBUTE_DECIMAL_FORMAT = DecimalFormat("#.##")
    .apply { decimalFormatSymbols = DecimalFormatSymbols.getInstance(Locale.ROOT) }

/**
 * Handles actions performed on [ItemStack]s of a [NovaItem]
 */
internal class ItemLogic internal constructor(holders: List<ItemBehaviorHolder<*>>) : Reloadable {
    
    private val behaviors: List<ItemBehavior> by lazy { holders.map { it.get(material) } }
    private lateinit var material: NovaItem
    private lateinit var name: Component
    
    lateinit var vanillaMaterial: Material
        private set
    lateinit var attributeModifiers: Map<EquipmentSlot, List<AttributeModifier>>
        private set
    
    internal constructor(vararg holders: ItemBehaviorHolder<*>) : this(holders.toList())
    
    override fun reload() {
        vanillaMaterial = VanillaMaterialTypes.getMaterial(behaviors.flatMap { it.getVanillaMaterialProperties() }.toHashSet())
        
        val modifiers = loadConfiguredAttributeModifiers() + behaviors.flatMap { it.getAttributeModifiers() }
        val modifiersBySlot = enumMap<EquipmentSlot, ArrayList<AttributeModifier>>()
        modifiers.forEach { modifier ->
            modifier.slots.forEach { slot ->
                modifiersBySlot.getOrPut(slot, ::ArrayList) += modifier
            }
        }
        attributeModifiers = modifiersBySlot
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : ItemBehavior> getBehavior(type: KClass<T>): T? {
        return behaviors.firstOrNull { type.isSuperclassOf(it::class) } as T?
    }
    
    fun hasBehavior(type: KClass<out ItemBehavior>): Boolean {
        return behaviors.any { it::class == type }
    }
    
    fun setMaterial(material: NovaItem) {
        if (::material.isInitialized)
            throw IllegalStateException("NovaItems cannot be used for multiple materials")
        
        this.material = material
        this.name = Component.translatable(material.localizedName)
    }
    
    fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
        var builder = itemBuilder
        behaviors.forEach { builder = it.modifyItemBuilder(builder) }
        return builder
    }
    
    fun getPacketItemData(player: Player?, itemStack: MojangStack?): PacketItemData {
        val itemData = PacketItemData(itemStack?.orCreateTag ?: CompoundTag())
        
        behaviors.forEach { it.updatePacketItemData(itemStack?.novaCompound ?: NamespacedCompound(), itemData) }
        if (itemStack != null) itemData.addLore(generateAttributeModifiersTooltip(player?.serverPlayer, itemStack))
        if (itemData.name == null) itemData.name = this.name
        
        return itemData
    }
    
    //<editor-fold desc="event methods", defaultstate="collapsed">
    fun handleInteract(player: Player, itemStack: ItemStack, action: Action, event: PlayerInteractEvent) {
        behaviors.forEach { it.handleInteract(player, itemStack, action, event) }
    }
    
    fun handleEntityInteract(player: Player, itemStack: ItemStack, clicked: Entity, event: PlayerInteractAtEntityEvent) {
        behaviors.forEach { it.handleEntityInteract(player, itemStack, clicked, event) }
    }
    
    fun handleAttackEntity(player: Player, itemStack: ItemStack, attacked: Entity, event: EntityDamageByEntityEvent) {
        behaviors.forEach { it.handleAttackEntity(player, itemStack, attacked, event) }
    }
    
    fun handleBreakBlock(player: Player, itemStack: ItemStack, event: BlockBreakEvent) {
        behaviors.forEach { it.handleBreakBlock(player, itemStack, event) }
    }
    
    fun handleDamage(player: Player, itemStack: ItemStack, event: PlayerItemDamageEvent) {
        behaviors.forEach { it.handleDamage(player, itemStack, event) }
    }
    
    fun handleBreak(player: Player, itemStack: ItemStack, event: PlayerItemBreakEvent) {
        behaviors.forEach { it.handleBreak(player, itemStack, event) }
    }
    
    fun handleEquip(player: Player, itemStack: ItemStack, equipped: Boolean, event: ArmorEquipEvent) {
        behaviors.forEach { it.handleEquip(player, itemStack, equipped, event) }
    }
    
    fun handleInventoryClick(player: Player, itemStack: ItemStack, event: InventoryClickEvent) {
        behaviors.forEach { it.handleInventoryClick(player, itemStack, event) }
    }
    
    fun handleInventoryClickOnCursor(player: Player, itemStack: ItemStack, event: InventoryClickEvent) {
        behaviors.forEach { it.handleInventoryClickOnCursor(player, itemStack, event) }
    }
    
    fun handleInventoryHotbarSwap(player: Player, itemStack: ItemStack, event: InventoryClickEvent) {
        behaviors.forEach { it.handleInventoryHotbarSwap(player, itemStack, event) }
    }
    
    fun handleBlockBreakAction(player: Player, itemStack: ItemStack, event: BlockBreakActionEvent) {
        behaviors.forEach { it.handleBlockBreakAction(player, itemStack, event) }
    }
    
    fun handleRelease(player: Player, itemStack: ItemStack, event: ServerboundPlayerActionPacketEvent) {
        behaviors.forEach { it.handleRelease(player, itemStack, event) }
    }
    //</editor-fold>
    
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
                } catch (e: Exception) {
                    LOGGER.logExceptionMessages(Level.WARNING, "Failed to load attribute modifier for $material", e)
                }
            }
        
        return modifiers
    }
    
    private fun generateAttributeModifiersTooltip(player: ServerPlayer?, itemStack: MojangStack): List<Component> {
        if (HideableFlag.MODIFIERS.isHidden(itemStack.tag?.getInt("HideFlags") ?: 0))
            return emptyList()
        
        // if the item has custom modifiers set, all default modifiers are ignored
        val customModifiers = itemStack.tag?.contains("AttributeModifiers", Tag.TAG_LIST.toInt()) == true
        
        val lore = ArrayList<Component>()
        EquipmentSlot.values().forEach { slot ->
            val modifiers = if (customModifiers)
                ItemUtils.getCustomAttributeModifiers(itemStack, slot)
            else attributeModifiers[slot] ?: emptyList()
            
            if (modifiers.isEmpty() || modifiers.none { it.showInLore && it.value != 0.0 })
                return@forEach
            
            lore += Component.empty()
            lore += Component.translatable("item.modifiers.${slot.name.lowercase()}", NamedTextColor.GRAY)
            
            modifiers.asSequence()
                .filter { it.showInLore && it.value != 0.0 }
                .forEach { modifier ->
                    var value = modifier.value
                    var isBaseModifier = false
                    
                    when (modifier.uuid) {
                        Tool.BASE_ATTACK_DAMAGE_UUID -> {
                            value += player?.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) ?: 1.0
                            value += EnchantmentHelper.getDamageBonus(itemStack, MobType.UNDEFINED)
                            isBaseModifier = true
                        }
                        
                        Tool.BASE_ATTACK_SPEED_UUID -> {
                            value += player?.getAttributeBaseValue(Attributes.ATTACK_SPEED) ?: 4.0
                            isBaseModifier = true
                        }
                    }
                    
                    var displayedValue = if (modifier.operation == Operation.ADDITION) {
                        if (modifier.attribute == Attributes.KNOCKBACK_RESISTANCE) {
                            value * 10.0 // no idea why, but this is how it is done in vanilla
                        } else value
                    } else value * 100.0
                    
                    fun appendModifier(type: String, color: TextColor) {
                        lore += Component.text()
                            .append(Component.text(if (isBaseModifier) " " else ""))
                            .append(Component.translatable(
                                "attribute.modifier.$type.${modifier.operation.ordinal}",
                                color,
                                Component.text(ATTRIBUTE_DECIMAL_FORMAT.format(displayedValue)),
                                Component.translatable(modifier.attribute.descriptionId)
                            ))
                            .build()
                    }
                    
                    if (isBaseModifier) {
                        appendModifier("equals", NamedTextColor.DARK_GREEN)
                    } else if (value > 0.0) {
                        appendModifier("plus", NamedTextColor.BLUE)
                    } else if (value < 0.0) {
                        displayedValue *= -1
                        appendModifier("take", NamedTextColor.RED)
                    }
                }
        }
        
        return lore
    }
    
}