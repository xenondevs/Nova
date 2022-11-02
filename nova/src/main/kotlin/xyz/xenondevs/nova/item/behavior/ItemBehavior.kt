package xyz.xenondevs.nova.item.behavior

import de.studiocode.invui.item.builder.ItemBuilder
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
import xyz.xenondevs.nmsutils.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nova.data.provider.Provider
import xyz.xenondevs.nova.data.provider.provider
import xyz.xenondevs.nova.item.PacketItemData
import xyz.xenondevs.nova.item.vanilla.AttributeModifier
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.player.equipment.ArmorEquipEvent

abstract class ItemBehavior : ItemBehaviorHolder<ItemBehavior>() {
    
    open val vanillaMaterialProperties: Provider<List<VanillaMaterialProperty>> = provider(emptyList())
    open val attributeModifiers: Provider<List<AttributeModifier>> = provider(emptyList())
    
    open fun handleInteract(player: Player, itemStack: ItemStack, action: Action, event: PlayerInteractEvent) = Unit
    open fun handleEntityInteract(player: Player, itemStack: ItemStack, clicked: Entity, event: PlayerInteractAtEntityEvent) = Unit
    open fun handleAttackEntity(player: Player, itemStack: ItemStack, attacked: Entity, event: EntityDamageByEntityEvent) = Unit
    open fun handleBreakBlock(player: Player, itemStack: ItemStack, event: BlockBreakEvent) = Unit
    open fun handleDamage(player: Player, itemStack: ItemStack, event: PlayerItemDamageEvent) = Unit
    open fun handleBreak(player: Player, itemStack: ItemStack, event: PlayerItemBreakEvent) = Unit
    open fun handleEquip(player: Player, itemStack: ItemStack, equipped: Boolean, event: ArmorEquipEvent) = Unit
    open fun handleInventoryClick(player: Player, itemStack: ItemStack, event: InventoryClickEvent) = Unit
    open fun handleInventoryClickOnCursor(player: Player, itemStack: ItemStack, event: InventoryClickEvent) = Unit
    open fun handleInventoryHotbarSwap(player: Player, itemStack: ItemStack, event: InventoryClickEvent) = Unit
    open fun handleRelease(player: Player, itemStack: ItemStack, event: ServerboundPlayerActionPacketEvent) = Unit
    
    open fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder = itemBuilder
    open fun updatePacketItemData(itemStack: ItemStack, itemData: PacketItemData) = Unit
    
    final override fun get(material: ItemNovaMaterial): ItemBehavior = this
    
}

abstract class ItemBehaviorFactory<T : ItemBehavior> : ItemBehaviorHolder<T>() {
    internal abstract fun create(material: ItemNovaMaterial): T
    final override fun get(material: ItemNovaMaterial) = create(material)
}

abstract class ItemBehaviorHolder<T : ItemBehavior> internal constructor() {
    internal abstract fun get(material: ItemNovaMaterial): T
}