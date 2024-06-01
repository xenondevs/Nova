package xyz.xenondevs.nova.item.behavior

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.resources.ResourceLocation
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.item.DefaultItems
import xyz.xenondevs.nova.item.logic.PacketItemData
import xyz.xenondevs.nova.player.WrappedPlayerInteractEvent
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.tileentity.network.type.item.ItemFilter
import xyz.xenondevs.nova.util.item.novaCompound
import net.minecraft.world.item.ItemStack as MojangStack
import org.bukkit.inventory.ItemStack as BukkitStack

private val ID_KEY = ResourceLocation("nova", "unknown_item_filter_original_id")
private val DATA_KEY = ResourceLocation("nova", "unknown_item_filter_original_data")

internal object UnknownItemFilterBehavior : ItemBehavior, ItemFilterContainer<UnknownItemFilter> {
    
    override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, wrappedEvent: WrappedPlayerInteractEvent) {
        val event = wrappedEvent.event
        
        val data = itemStack.novaCompound
        val filterType = NovaRegistries.ITEM_FILTER_TYPE[data.get<ResourceLocation>(ID_KEY)] ?: return
        val filterStack = filterType.deserialize(data.get<Compound>(DATA_KEY)!!)
            .toItemStack()
            .apply { amount = itemStack.amount }
        
        player.inventory.setItem(event.hand!!, filterStack)
        
        event.isCancelled = true
        wrappedEvent.actionPerformed = true
    }
    
    override fun getFilter(itemStack: ItemStack): UnknownItemFilter {
        val data = itemStack.novaCompound
        return UnknownItemFilter(data[ID_KEY]!!, data[DATA_KEY]!!)
    }
    
    override fun setFilter(itemStack: ItemStack, filter: UnknownItemFilter?) {
        if (filter != null) {
            itemStack.novaCompound[ID_KEY] = filter.originalId
            itemStack.novaCompound[DATA_KEY] = filter.originalData
        } else {
            itemStack.novaCompound.remove(ID_KEY)
            itemStack.novaCompound.remove(DATA_KEY)
        }
    }
    
    override fun updatePacketItemData(data: NamespacedCompound, itemData: PacketItemData) {
        itemData.addLore(
            Component.translatable("item.nova.unknown_item_filter.description", NamedTextColor.RED),
            Component.text(data.get<ResourceLocation>(ID_KEY).toString(), NamedTextColor.GRAY)
        )
        itemData.addLore(
            data.get<Compound>(DATA_KEY).toString()
                .lineSequence()
                .flatMap { it.chunkedSequence(100) }
                .map { Component.text(it, NamedTextColor.GRAY) }
                .toList()
        )
    }
    
}

internal class UnknownItemFilter(
    val originalId: ResourceLocation,
    val originalData: Compound
) : ItemFilter<UnknownItemFilter> {
    
    override val type
        get() = throw UnsupportedOperationException()
    
    override fun allows(itemStack: MojangStack) = false
    
    override fun toItemStack(): BukkitStack {
        val itemStack = DefaultItems.UNKNOWN_ITEM_FILTER.createItemStack()
        UnknownItemFilterBehavior.setFilter(itemStack, this)
        return itemStack
    }
    
}