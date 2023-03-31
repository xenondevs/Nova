@file:Suppress("FunctionName")

package xyz.xenondevs.nova.item.behavior

import net.minecraft.nbt.CompoundTag
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation
import net.minecraft.world.entity.ai.attributes.Attributes
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.item.PacketItemData
import xyz.xenondevs.nova.item.vanilla.AttributeModifier
import xyz.xenondevs.nova.item.vanilla.HideableFlag
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.material.NovaItem
import xyz.xenondevs.nova.material.options.WearableOptions
import xyz.xenondevs.nova.player.equipment.ArmorType
import xyz.xenondevs.nova.util.data.getOrPut
import xyz.xenondevs.nova.util.item.isActuallyInteractable
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.nmsCopy
import xyz.xenondevs.nova.util.nmsEquipmentSlot
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.util.swingHand

fun Wearable(type: ArmorType, equipSound: Sound): ItemBehaviorFactory<Wearable> =
    Wearable(type, equipSound.key.toString())

fun Wearable(type: ArmorType, equipSound: SoundEvent): ItemBehaviorFactory<Wearable> =
    Wearable(type, equipSound.location.toString())

fun Wearable(type: ArmorType, equipSound: String? = null): ItemBehaviorFactory<Wearable> =
    object : ItemBehaviorFactory<Wearable>() {
        override fun create(material: NovaItem): Wearable =
            Wearable(WearableOptions.configurable(type, equipSound, material))
    }

class Wearable(val options: WearableOptions) : ItemBehavior() {
    
    private val textureColor: Int? by lazy {
        Resources.getModelData(novaMaterial.id).armor
            ?.let { Resources.getArmorData(it) }?.color
    }
    
    override fun getVanillaMaterialProperties(): List<VanillaMaterialProperty> {
        if (textureColor == null)
            return emptyList()
        
        return listOf(
            when (options.armorType) {
                ArmorType.HELMET -> VanillaMaterialProperty.HELMET
                ArmorType.CHESTPLATE -> VanillaMaterialProperty.CHESTPLATE
                ArmorType.LEGGINGS -> VanillaMaterialProperty.LEGGINGS
                ArmorType.BOOTS -> VanillaMaterialProperty.BOOTS
            }
        )
    }
    
    override fun getAttributeModifiers(): List<AttributeModifier> {
        val equipmentSlot = options.armorType.equipmentSlot.nmsEquipmentSlot
        return listOf(
            AttributeModifier(
                "Nova Armor (${novaMaterial.id}})",
                Attributes.ARMOR,
                Operation.ADDITION,
                options.armor,
                true,
                equipmentSlot
            ),
            AttributeModifier(
                "Nova Armor Toughness (${novaMaterial.id}})",
                Attributes.ARMOR_TOUGHNESS,
                Operation.ADDITION,
                options.armorToughness,
                true,
                equipmentSlot
            ),
            AttributeModifier(
                "Nova Knockback Resistance (${novaMaterial.id}})",
                Attributes.KNOCKBACK_RESISTANCE,
                Operation.ADDITION,
                options.knockbackResistance,
                true,
                equipmentSlot
            )
        )
    }
    
    override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, event: PlayerInteractEvent) {
        if (action == Action.RIGHT_CLICK_AIR || (action == Action.RIGHT_CLICK_BLOCK && !event.clickedBlock!!.type.isActuallyInteractable())) {
            event.isCancelled = true
            
            val hand = event.hand!!
            val equipmentSlot = options.armorType.equipmentSlot
            val previous = player.inventory.getItem(equipmentSlot)?.takeUnlessEmpty()
            if (previous != null) {
                // swap armor
                player.inventory.setItem(equipmentSlot, itemStack)
                player.inventory.setItem(hand, previous)
            } else {
                // equip armor
                player.inventory.setItem(equipmentSlot, itemStack)
                if (player.gameMode != GameMode.CREATIVE) player.inventory.setItem(hand, null)
            }
            
            player.swingHand(hand)
            player.serverPlayer.onEquipItem(options.armorType.equipmentSlot.nmsEquipmentSlot, previous.nmsCopy, itemStack.nmsCopy)
        }
    }
    
    override fun updatePacketItemData(data: NamespacedCompound, itemData: PacketItemData) {
        val textureColor = textureColor
        if (textureColor != null) {
            itemData.nbt.getOrPut("display", ::CompoundTag).putInt("color", textureColor)
            itemData.hide(HideableFlag.DYE)
        }
        textureColor?.let { itemData.nbt.getOrPut("display", ::CompoundTag).putInt("color", it) }
    }
    
}