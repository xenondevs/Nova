package xyz.xenondevs.nova.world.item.behavior

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.util.component.adventure.withoutPreFormatting
import xyz.xenondevs.nova.util.getValue
import xyz.xenondevs.nova.util.item.novaCompound
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemFilter
import xyz.xenondevs.nova.world.item.DefaultItems
import xyz.xenondevs.nova.world.player.WrappedPlayerInteractEvent

private val ID_KEY = Key.key("nova", "unknown_item_filter_original_id")
private val DATA_KEY = Key.key("nova", "unknown_item_filter_original_data")

internal object UnknownItemFilterBehavior : ItemBehavior, ItemFilterContainer<UnknownItemFilter> {
    
    override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, wrappedEvent: WrappedPlayerInteractEvent) {
        val event = wrappedEvent.event
        
        val data = itemStack.novaCompound ?: return
        val filterType = NovaRegistries.ITEM_FILTER_TYPE.getValue(data.get<Key>(ID_KEY)) ?: return
        val filterStack = filterType.deserialize(data.get<Compound>(DATA_KEY)!!)
            .toItemStack()
            .apply { amount = itemStack.amount }
        
        player.inventory.setItem(event.hand!!, filterStack)
        
        event.isCancelled = true
        wrappedEvent.actionPerformed = true
    }
    
    override fun getFilter(itemStack: ItemStack): UnknownItemFilter {
        val data = itemStack.novaCompound!!
        return UnknownItemFilter(data[ID_KEY]!!, data[DATA_KEY]!!)
    }
    
    override fun setFilter(itemStack: ItemStack, filter: UnknownItemFilter?) {
        var compound = itemStack.novaCompound
        if (filter != null) {
            compound = compound ?: NamespacedCompound()
            compound[ID_KEY] = filter.originalId
            compound[DATA_KEY] = filter.originalData
            itemStack.novaCompound = compound
        } else if (compound != null) {
            compound.remove(ID_KEY)
            compound.remove(DATA_KEY)
            itemStack.novaCompound = compound
        }
    }
    
    override fun modifyClientSideStack(player: Player?, itemStack: ItemStack, data: NamespacedCompound): ItemStack {
        val lore = ArrayList<Component>()
        lore += Component.translatable("item.nova.unknown_item_filter.description", NamedTextColor.RED)
        lore += Component.text(data.get<Key>(ID_KEY).toString(), NamedTextColor.GRAY)
        lore += data.get<Compound>(DATA_KEY).toString()
            .lineSequence()
            .flatMap { it.chunkedSequence(100) }
            .map { Component.text(it, NamedTextColor.GRAY) }
            .toList()
        itemStack.lore((itemStack.lore() ?: emptyList()) + lore.map(Component::withoutPreFormatting))
        return itemStack
    }
    
    override fun toString(itemStack: ItemStack): String {
        return "UnknownItemFilterBehavior(" +
            "id=${itemStack.novaCompound?.get<Key>(ID_KEY)}, " +
            "data=${itemStack.novaCompound?.get<Compound>(DATA_KEY)})"
        ")"
    }
    
}

internal class UnknownItemFilter(
    val originalId: Key,
    val originalData: Compound
) : ItemFilter<UnknownItemFilter> {
    
    override val type
        get() = throw UnsupportedOperationException()
    
    override fun allows(itemStack: ItemStack) = false
    
    override fun toItemStack(): ItemStack {
        val itemStack = DefaultItems.UNKNOWN_ITEM_FILTER.createItemStack()
        UnknownItemFilterBehavior.setFilter(itemStack, this)
        return itemStack
    }
    
}