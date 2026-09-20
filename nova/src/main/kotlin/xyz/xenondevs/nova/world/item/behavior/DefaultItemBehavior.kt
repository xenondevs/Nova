package xyz.xenondevs.nova.world.item.behavior

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemEnchantments.itemEnchantments
import io.papermc.paper.datacomponent.item.ItemLore.lore
import io.papermc.paper.datacomponent.item.TooltipDisplay.tooltipDisplay
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.NULL_PROVIDER
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.context.intention.EntityInteract
import xyz.xenondevs.nova.context.intention.ItemUse
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.util.asBukkitMirror
import xyz.xenondevs.nova.util.component.adventure.toNmsStyle
import xyz.xenondevs.nova.util.item.update
import xyz.xenondevs.nova.util.nmsDirection
import xyz.xenondevs.nova.util.nmsEntity
import xyz.xenondevs.nova.util.nmsInteractionHand
import xyz.xenondevs.nova.util.nmsPos
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.item.DataComponentMap
import xyz.xenondevs.nova.world.item.DefaultItemTags
import xyz.xenondevs.nova.world.item.ItemAction
import xyz.xenondevs.nova.world.item.TooltipStyle
import xyz.xenondevs.nova.world.item.buildDataComponentMap
import xyz.xenondevs.nova.world.toNova
import net.minecraft.network.chat.Component as MojangComponent

internal class DefaultItemBehavior(
    id: Key,
    name: Component?,
    style: Style,
    lore: List<Component>,
    tooltipStyle: RegistryEntry.Nova<TooltipStyle>?,
    maxStackSize: Int,
    isHidden: Boolean
) : ItemBehavior {
    
    private val style = style.toNmsStyle()
    
    override val tags = provider(if (isHidden) setOf(DefaultItemTags.NOVA_HIDDEN) else setOf(DefaultItemTags.NOVA))
    
    override val baseDataComponents: Provider<DataComponentMap> = (tooltipStyle ?: NULL_PROVIDER).map { tooltipStyle ->
        buildDataComponentMap {
            if (name != null) {
                this[DataComponentTypes.ITEM_NAME] = name.style(name.style().merge(style))
            } else {
                this[DataComponentTypes.TOOLTIP_DISPLAY] = tooltipDisplay().hideTooltip(true).build()
            }
            
            if (lore.isNotEmpty()) {
                this[DataComponentTypes.LORE] = lore(lore)
            }
            
            if (tooltipStyle != null) {
                this[DataComponentTypes.TOOLTIP_STYLE] = tooltipStyle.key
            }
            
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
    
    override fun useOnBlock(itemStack: ItemStack, block: Block, ctx: Context<BlockInteract>): InteractionResult {
        val player = ctx[BlockInteract.SOURCE_PLAYER]
            ?: return InteractionResult.Pass
        val hand = ctx[BlockInteract.HELD_HAND]
            ?: return InteractionResult.Pass
        val face = ctx[BlockInteract.CLICKED_BLOCK_FACE]
            ?: return InteractionResult.Pass
        
        // run default data component functionality (of block transformers, etc.)
        val nmsContext = UseOnContext(
            player.serverPlayer,
            hand.nmsInteractionHand,
            BlockHitResult(Vec3.atCenterOf(block.nmsPos), face.nmsDirection, block.nmsPos, false)
        )
        val result = itemStack.unwrap().item.useOn(nmsContext).toNova()
        if (result !is InteractionResult.Success)
            return result
        
        // reset to previous item so that component-based post-use effects in InteractionResult.Success#performActions work correctly
        val action = ItemAction.ConvertStack(player.equipment.getItem(hand))
        player.equipment.setItem(hand, itemStack)
        return result.copy(action = action)
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
