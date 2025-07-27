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
import org.bukkit.event.player.PlayerInteractEvent
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
    
    /**
     * Called when a [PlayerInteractEvent] is fired for an [itemStack] with this behavior.
     */
    fun handleInteract(player: Player, itemStack: ItemStack, action: Action, wrappedEvent: WrappedPlayerInteractEvent) = Unit
    
    /**
     * Called when a [PlayerInteractAtEntityEvent] is fired for an [itemStack] with this behavior.
     */
    fun handleEntityInteract(player: Player, itemStack: ItemStack, clicked: Entity, event: PlayerInteractAtEntityEvent) = Unit
    
    /**
     * Called when an [EntityDamageByEntityEvent] is fired where [player] attacks [attacked] using [itemStack] with this behavior in their main hand.
     */
    fun handleAttackEntity(player: Player, itemStack: ItemStack, attacked: Entity, event: EntityDamageByEntityEvent) = Unit
    
    /**
     * Called when a [BlockBreakEvent] is fired where [player] uses an [itemStack] with this behavior.
     */
    fun handleBreakBlock(player: Player, itemStack: ItemStack, event: BlockBreakEvent) = Unit
    
    /**
     * Called when a [PlayerItemDamageEvent] is fired where an [itemStack] with this behavior takes damage.
     */
    fun handleDamage(player: Player, itemStack: ItemStack, event: PlayerItemDamageEvent) = Unit
    
    /**
     * Called when a a[PlayerItemBreakEvent] is fired where an [itemStack] with this behavior runs out of durability and breaks.
     */
    fun handleBreak(player: Player, itemStack: ItemStack, event: PlayerItemBreakEvent) = Unit
    
    /**
     * Called when a [EntityEquipmentChangedEvent] is fired where [player] equips or unequips an [itemStack] with this behavior.
     */
    fun handleEquip(player: Player, itemStack: ItemStack, slot: EquipmentSlot, equipped: Boolean, event: EntityEquipmentChangedEvent) = Unit
    
    /**
     * Called when an [InventoryClickEvent] is fired where [player] clicks on an [itemStack] with this behavior.
     */
    fun handleInventoryClick(player: Player, itemStack: ItemStack, event: InventoryClickEvent) = Unit
    
    /**
     * Called when an [InventoryClickEvent] is fired where [player] clicks on an [itemStack] with this behavior in their cursor.
     */
    fun handleInventoryClickOnCursor(player: Player, itemStack: ItemStack, event: InventoryClickEvent) = Unit
    
    /**
     * Called when an [InventoryClickEvent] is fired where [player] swaps an item with a hotbar [itemStack] with this behavior.
     */
    fun handleInventoryHotbarSwap(player: Player, itemStack: ItemStack, event: InventoryClickEvent) = Unit
    
    /**
     * Called when a [BlockBreakActionEvent] is fired where [player] breaks a block with an [itemStack] with this behavior.
     */
    fun handleBlockBreakAction(player: Player, itemStack: ItemStack, event: BlockBreakActionEvent) = Unit
    
    /**
     * Called when a [PlayerItemConsumeEvent] is fired where [player] consumes an [itemStack] with this behavior.
     */
    fun handleConsume(player: Player, itemStack: ItemStack, event: PlayerItemConsumeEvent) = Unit
    
    /**
     * Called when a [ServerboundPlayerActionPacketEvent] is fired where the server receives a player action packet regarding an [itemStack] with this behavior.
     * Note that this is a packet event, and as such this function is called a netty thread, not the server thread.
     */
    fun handleRelease(player: Player, itemStack: ItemStack, event: ServerboundPlayerActionPacketEvent) = Unit
    
    /**
     * Called every tick for an [itemStack] with this behavior that is in the [player's][player] inventory.
     * The [slot] is the index inside the [player inventory's][Player.getInventory] [contents][org.bukkit.inventory.Inventory.getContents].
     */
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