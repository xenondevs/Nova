package xyz.xenondevs.nova.item.impl

import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.attachment.Attachment
import xyz.xenondevs.nova.attachment.AttachmentManager
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.equipment.event.ArmorEquipEvent
import xyz.xenondevs.nova.equipment.event.EquipMethod
import xyz.xenondevs.nova.material.NovaMaterial

private val MAX_ENERGY = NovaConfig.getInt("jetpack.capacity")!!

object JetpackItem : ChargeableItem(MAX_ENERGY) {
    
    override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, event: PlayerInteractEvent) {
        addEnergy(itemStack, 1000)
    }
    
    override fun handleEquip(player: Player, itemStack: ItemStack, equipped: Boolean, event: ArmorEquipEvent) {
        if (event.equipMethod == EquipMethod.BREAK) {
            event.isCancelled = true
        } else {
            if (equipped) Attachment("Jetpack", player.uniqueId, NovaMaterial.JETPACK.createItemStack(), true)
            else AttachmentManager.getAttachment(player.uniqueId, "Jetpack")?.remove()
        }
    }
    
}