package xyz.xenondevs.nova.item.behavior

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
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.nmsutils.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.item.PacketItemData
import xyz.xenondevs.nova.item.vanilla.AttributeModifier
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.material.NovaItem
import xyz.xenondevs.nova.player.equipment.ArmorEquipEvent
import xyz.xenondevs.nova.world.block.event.BlockBreakActionEvent

abstract class ItemBehavior : ItemBehaviorHolder<ItemBehavior>() {
    
    lateinit var novaMaterial: NovaItem
        internal set
    
    open fun getVanillaMaterialProperties(): List<VanillaMaterialProperty> = emptyList()
    open fun getAttributeModifiers(): List<AttributeModifier> = emptyList()
    
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
    open fun handleBlockBreakAction(player: Player, itemStack: ItemStack, event: BlockBreakActionEvent) = Unit
    open fun handleRelease(player: Player, itemStack: ItemStack, event: ServerboundPlayerActionPacketEvent) = Unit
    
    open fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder = itemBuilder
    open fun updatePacketItemData(data: NamespacedCompound, itemData: PacketItemData) = Unit
    
    final override fun get(material: NovaItem): ItemBehavior {
        setMaterial(material)
        return this
    }
    
    internal fun setMaterial(material: NovaItem) {
        if (::novaMaterial.isInitialized)
            throw IllegalStateException("The same item behavior instance cannot be used for multiple materials")
        
        novaMaterial = material
    }
    
}

abstract class ItemBehaviorFactory<T : ItemBehavior> : ItemBehaviorHolder<T>() {
    abstract fun create(material: NovaItem): T
    final override fun get(material: NovaItem) = create(material).apply { setMaterial(material) }
}

abstract class ItemBehaviorHolder<T : ItemBehavior> internal constructor() {
    internal abstract fun get(material: NovaItem): T
}