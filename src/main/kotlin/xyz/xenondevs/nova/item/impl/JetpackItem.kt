package xyz.xenondevs.nova.item.impl

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.player.ability.AbilityManager
import xyz.xenondevs.nova.player.ability.AbilityManager.AbilityType
import xyz.xenondevs.nova.player.attachment.Attachment
import xyz.xenondevs.nova.player.attachment.AttachmentManager
import xyz.xenondevs.nova.player.equipment.ArmorEquipEvent
import xyz.xenondevs.nova.player.equipment.EquipMethod

private val MAX_ENERGY = NovaConfig.getInt("jetpack.capacity")!!

object JetpackItem : ChargeableItem(MAX_ENERGY) {
    
    override fun handleEquip(player: Player, itemStack: ItemStack, equipped: Boolean, event: ArmorEquipEvent) {
        if (event.equipMethod == EquipMethod.BREAK) {
            event.isCancelled = true
        } else {
            if (equipped) {
                Attachment("Jetpack", player.uniqueId, NovaMaterial.JETPACK.createItemStack(), true)
                AbilityManager.giveAbility(player, AbilityType.JETPACK)
            } else {
                AttachmentManager.getAttachment(player.uniqueId, "Jetpack")?.remove()
                AbilityManager.takeAbility(player, AbilityType.JETPACK)
            }
        }
    }
    
}