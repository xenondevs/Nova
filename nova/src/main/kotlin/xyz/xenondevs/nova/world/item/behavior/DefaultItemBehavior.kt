package xyz.xenondevs.nova.world.item.behavior

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemEnchantments.itemEnchantments
import io.papermc.paper.datacomponent.item.ItemLore.lore
import io.papermc.paper.datacomponent.item.TooltipDisplay.tooltipDisplay
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.UseCooldown
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.NULL_PROVIDER
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.util.component.adventure.toNmsStyle
import xyz.xenondevs.nova.util.item.update
import xyz.xenondevs.nova.util.toIdentifier
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.item.DataComponentMap
import xyz.xenondevs.nova.world.item.DefaultItemTags
import xyz.xenondevs.nova.world.item.TooltipStyle
import xyz.xenondevs.nova.world.item.buildDataComponentMap
import java.util.*
import net.minecraft.network.chat.Component as MojangComponent

internal class DefaultItemBehavior(
    private val key: Key,
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
            this[DataComponentTypes.ITEM_MODEL] = key
            
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
        client.unwrap().update(DataComponents.USE_COOLDOWN) {
            if (it.cooldownGroup.isEmpty)
                UseCooldown(it.seconds, Optional.of(key.toIdentifier()))
            else it
        }
        return client
    }
    
}
