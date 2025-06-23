package xyz.xenondevs.nova.world.item.behavior

import io.papermc.paper.event.entity.EntityEquipmentChangedEvent
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nova.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.world.block.event.BlockBreakActionEvent
import xyz.xenondevs.nova.world.item.DataComponentMap
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.world.player.WrappedPlayerInteractEvent

/**
 * Either [ItemBehavior] or [ItemBehaviorFactory]
 */
sealed interface ItemBehaviorHolder

/**
 * Adds functionality to an item type.
 */
interface ItemBehavior : ItemBehaviorHolder {
    
    /**
     * The base data components that every item with this [ItemBehavior] has.
     */
    val baseDataComponents: Provider<DataComponentMap>
        get() = provider(DataComponentMap.EMPTY)
    
    /**
     * The [NamespacedCompound] that every new [ItemStack] of an item with this [ItemBehavior] has by default.
     */
    val defaultCompound: Provider<NamespacedCompound>
        get() = provider(NamespacedCompound())
    
    /**
     * The vanilla material properties that an item with this [ItemBehavior] requires.
     */
    val vanillaMaterialProperties: Provider<List<VanillaMaterialProperty>>
        get() = provider(emptyList())
    
    fun handleInteract(player: Player, itemStack: ItemStack, action: Action, wrappedEvent: WrappedPlayerInteractEvent) = Unit
    fun handleEntityInteract(player: Player, itemStack: ItemStack, clicked: Entity, event: PlayerInteractAtEntityEvent) = Unit
    fun handleAttackEntity(player: Player, itemStack: ItemStack, attacked: Entity, event: EntityDamageByEntityEvent) = Unit
    fun handleBreakBlock(player: Player, itemStack: ItemStack, event: BlockBreakEvent) = Unit
    fun handleDamage(player: Player, itemStack: ItemStack, event: PlayerItemDamageEvent) = Unit
    fun handleBreak(player: Player, itemStack: ItemStack, event: PlayerItemBreakEvent) = Unit
    fun handleEquip(player: Player, itemStack: ItemStack, slot: EquipmentSlot, equipped: Boolean, event: EntityEquipmentChangedEvent) = Unit
    fun handleInventoryClick(player: Player, itemStack: ItemStack, event: InventoryClickEvent) = Unit
    fun handleInventoryClickOnCursor(player: Player, itemStack: ItemStack, event: InventoryClickEvent) = Unit
    fun handleInventoryHotbarSwap(player: Player, itemStack: ItemStack, event: InventoryClickEvent) = Unit
    fun handleBlockBreakAction(player: Player, itemStack: ItemStack, event: BlockBreakActionEvent) = Unit
    fun handleConsume(player: Player, itemStack: ItemStack, event: PlayerItemConsumeEvent) = Unit
    fun handleRelease(player: Player, itemStack: ItemStack, event: ServerboundPlayerActionPacketEvent) = Unit
    fun handleInventoryTick(player: Player, itemStack: ItemStack, slot: Int) = Unit
    
    /**
     * Modifies the [damage] when [player] is breaking a [block] with [itemStack].
     * This damage is applied to the block every tick until 1.0 is reached, at which point the block is destroyed.
     */
    fun modifyBlockDamage(player: Player, itemStack: ItemStack, block: Block, damage: Double): Double = damage
    
    /**
     * Updates the [client-side item stack][client] that is to be viewed by [player] in place of the [server-side item stack][server].
     */
    fun modifyClientSideStack(player: Player?, server: ItemStack, client: ItemStack): ItemStack = client
    
    /**
     * Creates a string representation of this [ItemBehavior] and its data in [itemStack].
     */
    fun toString(itemStack: ItemStack): String = this.javaClass.simpleName
    
}

/**
 * Creates [ItemBehavior] instances for [NovaItems][NovaItem].
 */
fun interface ItemBehaviorFactory<T : ItemBehavior> : ItemBehaviorHolder {
    
    /**
     * Creates a new [ItemBehavior] instance for the given [item].
     */
    fun create(item: NovaItem): T
    
}