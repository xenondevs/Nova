package xyz.xenondevs.nova.world.item.behavior

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.ItemUse
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.util.component.adventure.withoutPreFormatting
import xyz.xenondevs.nova.util.getValue
import xyz.xenondevs.nova.util.item.novaCompound
import xyz.xenondevs.nova.util.item.retrieveData
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemFilter
import xyz.xenondevs.nova.world.item.DefaultItems
import xyz.xenondevs.nova.world.item.ItemAction

private val ID_KEY = Key.key("nova", "unknown_item_filter_original_id")
private val DATA_KEY = Key.key("nova", "unknown_item_filter_original_data")

internal object UnknownItemFilterBehavior : ItemBehavior, ItemFilterContainer<UnknownItemFilter> {
    
    override fun use(itemStack: ItemStack, ctx: Context<ItemUse>): InteractionResult {
        val data = itemStack.novaCompound ?: return InteractionResult.Pass
        val filterType = NovaRegistries.ITEM_FILTER_TYPE.getValue(data.get<Key>(ID_KEY)) ?: return InteractionResult.Pass
        val filterStack = filterType.deserialize(data.get<Compound>(DATA_KEY)!!)
            .toItemStack()
            .apply { amount = itemStack.amount }
        
        return InteractionResult.Success(swing = true, action = ItemAction.ConvertStack(filterStack))
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
    
    override fun modifyClientSideStack(player: Player?, server: ItemStack, client: ItemStack): ItemStack {
        val lore = ArrayList<Component>()
        lore += Component.translatable("item.nova.unknown_item_filter.description", NamedTextColor.RED)
        lore += Component.text(server.retrieveData<Key>(ID_KEY).toString(), NamedTextColor.GRAY)
        lore += server.retrieveData<Compound>(DATA_KEY).toString()
            .lineSequence()
            .flatMap { it.chunkedSequence(100) }
            .map { Component.text(it, NamedTextColor.GRAY) }
            .toList()
        client.lore((client.lore() ?: emptyList()) + lore.map(Component::withoutPreFormatting))
        return client
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