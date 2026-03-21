package xyz.xenondevs.nova.world.item.behavior

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers.itemAttributes
import io.papermc.paper.datacomponent.item.ItemEnchantments.itemEnchantments
import io.papermc.paper.datacomponent.item.ItemLore.lore
import io.papermc.paper.datacomponent.item.TooltipDisplay.tooltipDisplay
import io.papermc.paper.datacomponent.item.attribute.AttributeModifierDisplay
import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.minecraft.core.component.DataComponents
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.nova.config.ConfigProvider
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.EntityInteract
import xyz.xenondevs.nova.context.intention.ItemUse
import xyz.xenondevs.nova.util.component.adventure.toNmsStyle
import xyz.xenondevs.nova.util.item.update
import xyz.xenondevs.nova.util.nmsEntity
import xyz.xenondevs.nova.util.nmsInteractionHand
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.util.toNamespacedKey
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.item.DataComponentMap
import xyz.xenondevs.nova.world.item.ItemAction
import xyz.xenondevs.nova.world.item.TooltipStyle
import xyz.xenondevs.nova.world.item.buildDataComponentMap
import xyz.xenondevs.nova.world.toNova
import net.minecraft.network.chat.Component as MojangComponent

internal class DefaultBehavior(
    id: Key,
    name: Provider<Component?>,
    style: Provider<Style>,
    lore: Provider<List<Component>>,
    tooltipStyle: Provider<TooltipStyle?>,
    maxStackSize: Provider<Int>,
    config: ConfigProvider,
) : ItemBehavior {
    
    private val style by style.map { it.toNmsStyle() }
    
    override val baseDataComponents: Provider<DataComponentMap> = combinedProvider(
        name, style, lore, tooltipStyle, maxStackSize, loadConfiguredAttributeModifiers(id, config)
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
                this[DataComponentTypes.TOOLTIP_STYLE] = tooltipStyle.key
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
    
    override fun useOnEntity(itemStack: ItemStack, entity: Entity, ctx: Context<EntityInteract>): InteractionResult {
        if (entity !is LivingEntity)
            return InteractionResult.Pass
        
        val player = ctx[EntityInteract.SOURCE_PLAYER]
            ?: return InteractionResult.Pass
        val hand = ctx[EntityInteract.HELD_HAND]
            ?: return InteractionResult.Pass
        
        // run default data component functionality (of equippable, etc.)
        val result = itemStack.unwrap().interactLivingEntity(
            player.serverPlayer,
            entity.nmsEntity,
            hand.nmsInteractionHand
        ).toNova()
        
        // reset to previous item so that component-based post-use effects in InteractionResult.Success#performActions work correctly
        // this asserts that transformations are stored in the interaction result via transformedTo
        player.equipment.setItem(hand, itemStack)
        return result
    }
    
    override fun use(itemStack: ItemStack, ctx: Context<ItemUse>): InteractionResult {
        val player = ctx[ItemUse.SOURCE_PLAYER]
            ?: return InteractionResult.Pass
        val hand = ctx[ItemUse.HELD_HAND]
            ?: return InteractionResult.Pass
        
        // run default data component functionality (of consumable, equippable, etc.)
        val serverPlayer = player.serverPlayer
        val result = itemStack.unwrap().item.use(
            serverPlayer.level(),
            serverPlayer,
            hand.nmsInteractionHand
        ).toNova()
        
        // reset to previous item so that component-based post-use effects in InteractionResult.Success#performActions work correctly
        // this asserts that transformations are stored in the interaction result via transformedTo
        player.equipment.setItem(hand, itemStack)
        return result
    }
    
    override fun handleUseFinished(entity: LivingEntity, itemStack: ItemStack, hand: EquipmentSlot): ItemAction {
        // run default data component functionality (of consumable, etc.)
        val nmsStack = itemStack.unwrap().copy()
        val nmsEntity = entity.nmsEntity
        val result = nmsStack.item.finishUsingItem(nmsStack, nmsEntity.level(), nmsEntity).asBukkitMirror()
        if (result == itemStack)
            return ItemAction.None
        return ItemAction.ConvertStack(result)
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
    
}

@Serializable
private class AttributesSurrogate(
    val id: Key? = null,
    val attribute: Attribute,
    val operation: AttributeModifier.Operation,
    val value: Double,
    val display: AttributeModifierDisplay = AttributeModifierDisplay.reset()
)

private fun loadConfiguredAttributeModifiers(key: Key, config: ConfigProvider): Provider<ItemAttributeModifiers> =
    config.entry<Map<EquipmentSlotGroup, AttributesSurrogate>>(emptyMap(), "attribute_modifiers").map {
        val builder = itemAttributes()
        for ((slotGroup, attributes) in it) {
            val id = attributes.id
                ?: Key.key(key.namespace(), "${key.value()}_${slotGroup.toString().lowercase()}")
            
            builder.addModifier(
                attributes.attribute,
                AttributeModifier(
                    id.toNamespacedKey(),
                    attributes.value,
                    attributes.operation,
                    slotGroup
                ),
                attributes.display
            )
        }
        return@map builder.build()
    }