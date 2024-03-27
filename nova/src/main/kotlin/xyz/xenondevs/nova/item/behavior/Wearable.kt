@file:Suppress("FunctionName")

package xyz.xenondevs.nova.item.behavior

import net.minecraft.nbt.CompoundTag
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Equipable
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.inventory.EquipmentSlot
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.map
import xyz.xenondevs.commons.provider.immutable.orElse
import xyz.xenondevs.commons.provider.immutable.provider
import xyz.xenondevs.nova.data.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.armor.Armor
import xyz.xenondevs.nova.item.logic.PacketItemData
import xyz.xenondevs.nova.item.vanilla.AttributeModifier
import xyz.xenondevs.nova.item.vanilla.HideableFlag
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.player.WrappedPlayerInteractEvent
import xyz.xenondevs.nova.util.bukkitEquipmentSlot
import xyz.xenondevs.nova.util.data.getOrPut
import xyz.xenondevs.nova.util.item.isActuallyInteractable
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.nmsCopy
import xyz.xenondevs.nova.util.nmsEquipmentSlot
import xyz.xenondevs.nova.util.serverPlayer
import java.util.*
import net.minecraft.world.entity.EquipmentSlot as MojangEquipmentSlot
import net.minecraft.world.item.ItemStack as MojangStack
import org.bukkit.inventory.EquipmentSlot as BukkitEquipmentSlot
import org.bukkit.inventory.ItemStack as BukkitStack

fun Wearable(armor: Armor?, slot: BukkitEquipmentSlot, equipSound: Sound): ItemBehaviorFactory<Wearable.Default> =
    Wearable(armor, slot, equipSound.key.toString())

fun Wearable(armor: Armor?, slot: BukkitEquipmentSlot, equipSound: SoundEvent): ItemBehaviorFactory<Wearable.Default> =
    Wearable(armor, slot, equipSound.location.toString())

fun Wearable(armor: Armor?, slot: BukkitEquipmentSlot, equipSound: String? = null): ItemBehaviorFactory<Wearable.Default> {
    return object : ItemBehaviorFactory<Wearable.Default> {
        override fun create(item: NovaItem): Wearable.Default {
            val cfg = item.config
            return Wearable.Default(
                ResourceLookups.ARMOR_COLOR_LOOKUP.provider.map { map -> armor?.let(map::get) },
                provider(slot),
                cfg.optionalEntry<Double>("armor").orElse(0.0),
                cfg.optionalEntry<Double>("armor_toughness").orElse(0.0),
                cfg.optionalEntry<Double>("knockback_resistance").orElse(0.0),
                provider(equipSound)
            )
        }
    }
}

private val EquipmentSlot.inventorySlot
    get() = when (this) {
        EquipmentSlot.HEAD -> 5
        EquipmentSlot.CHEST -> 6
        EquipmentSlot.LEGS -> 7
        EquipmentSlot.FEET -> 8
        else -> throw UnsupportedOperationException()
    }

/**
 * Allows items to be worn in armor slots.
 */
sealed interface Wearable {
    
    val texture: Int?
    val slot: BukkitEquipmentSlot
    val armor: Double
    val armorToughness: Double
    val knockbackResistance: Double
    val equipSound: String?
    
    class Default(
        texture: Provider<Int?>,
        slot: Provider<BukkitEquipmentSlot>,
        armor: Provider<Double>,
        armorToughness: Provider<Double>,
        knockbackResistance: Provider<Double>,
        equipSound: Provider<String?>
    ) : ItemBehavior, Wearable {
        
        override val texture by texture
        override val slot by slot
        override val armor by armor
        override val armorToughness by armorToughness
        override val knockbackResistance by knockbackResistance
        override val equipSound by equipSound
        
        override fun getVanillaMaterialProperties(): List<VanillaMaterialProperty> {
            if (texture == null)
                return emptyList()
            
            return listOf(
                when (slot) {
                    BukkitEquipmentSlot.HEAD -> VanillaMaterialProperty.HELMET
                    BukkitEquipmentSlot.CHEST -> VanillaMaterialProperty.CHESTPLATE
                    BukkitEquipmentSlot.LEGS -> VanillaMaterialProperty.LEGGINGS
                    BukkitEquipmentSlot.FEET -> VanillaMaterialProperty.BOOTS
                    else -> throw IllegalArgumentException("Invalid wearable slot: $slot")
                }
            )
        }
        
        override fun getAttributeModifiers(): List<AttributeModifier> {
            val equipmentSlot = slot.nmsEquipmentSlot
            return listOf(
                AttributeModifier(
                    ARMOR_MODIFIER_UUIDS[slot]!!,
                    "Nova Wearable Armor",
                    Attributes.ARMOR,
                    Operation.ADDITION,
                    armor,
                    true,
                    equipmentSlot
                ),
                AttributeModifier(
                    ARMOR_TOUGHNESS_MODIFIER_UUIDS[slot]!!,
                    "Nova Wearable Armor Toughness",
                    Attributes.ARMOR_TOUGHNESS,
                    Operation.ADDITION,
                    armorToughness,
                    true,
                    equipmentSlot
                ),
                AttributeModifier(
                    KNOCKBACK_RESISTANCE_MODIFIER_UUIDS[slot]!!,
                    "Nova Wearable Knockback Resistance",
                    Attributes.KNOCKBACK_RESISTANCE,
                    Operation.ADDITION,
                    knockbackResistance,
                    true,
                    equipmentSlot
                )
            )
        }
        
        override fun handleInteract(player: Player, itemStack: BukkitStack, action: Action, wrappedEvent: WrappedPlayerInteractEvent) {
            val event = wrappedEvent.event
            val inventory = player.inventory
            if (!wrappedEvent.actionPerformed && (action == Action.RIGHT_CLICK_AIR || (action == Action.RIGHT_CLICK_BLOCK && !event.clickedBlock!!.type.isActuallyInteractable()))) {
                event.isCancelled = true
                
                val hand = event.hand!!
                val previous = inventory.getItem(slot).takeUnlessEmpty()
                if (previous != null) {
                    // swap armor
                    inventory.setItem(slot, itemStack)
                    inventory.setItem(hand, previous)
                } else {
                    // equip armor
                    inventory.setItem(slot, itemStack)
                    if (player.gameMode != GameMode.CREATIVE) inventory.setItem(hand, null)
                }
                
                player.swingHand(hand)
                player.serverPlayer.onEquipItem(slot.nmsEquipmentSlot, previous.nmsCopy, itemStack.nmsCopy)
                wrappedEvent.actionPerformed = true
            } else {
                // basically marks the remote armor slot as dirty, see https://hub.spigotmc.org/jira/browse/SPIGOT-7500
                player.serverPlayer.inventoryMenu.setRemoteSlot(slot.inventorySlot, itemStack.nmsCopy)
            }
        }
        
        override fun updatePacketItemData(data: NamespacedCompound, itemData: PacketItemData) {
            val texture = texture
            if (texture != null) {
                itemData.nbt.getOrPut("display", ::CompoundTag).putInt("color", texture)
                itemData.hide(HideableFlag.DYE)
            }
        }
        
    }
    
    companion object {
        
        val ARMOR_MODIFIER_UUIDS: Map<BukkitEquipmentSlot, UUID> = EquipmentSlot.entries.associateWithTo(enumMap()) { UUID.randomUUID() }
        val ARMOR_TOUGHNESS_MODIFIER_UUIDS: Map<BukkitEquipmentSlot, UUID> = EquipmentSlot.entries.associateWithTo(enumMap()) { UUID.randomUUID() }
        val KNOCKBACK_RESISTANCE_MODIFIER_UUIDS: Map<BukkitEquipmentSlot, UUID> = EquipmentSlot.entries.associateWithTo(enumMap()) { UUID.randomUUID() }
        
        /**
         * Checks whether the specified [itemStack] is wearable.
         */
        fun isWearable(itemStack: BukkitStack): Boolean =
            isWearable(itemStack.nmsCopy)
        
        /**
         * Checks whether the specified [itemStack] is wearable.
         */
        fun isWearable(itemStack: MojangStack): Boolean {
            val novaItem = itemStack.novaItem
            if (novaItem != null)
                return novaItem.hasBehavior<Wearable>()
            
            val item = itemStack.item
            return item is Equipable || item is BlockItem && item.block is Equipable
        }
        
        /**
         * Gets the [BukkitEquipmentSlot] of the specified [itemStack], or null if it is not wearable.
         */
        fun getSlot(itemStack: BukkitStack): BukkitEquipmentSlot? =
            getSlot(itemStack.nmsCopy)?.bukkitEquipmentSlot
        
        /**
         * Gets the [MojangEquipmentSlot] of the specified [itemStack], or null if it is not wearable.
         */
        fun getSlot(itemStack: MojangStack): MojangEquipmentSlot? {
            val novaItem = itemStack.novaItem
            if (novaItem != null)
                return novaItem.getBehaviorOrNull<Wearable>()?.slot?.nmsEquipmentSlot
            
            val equipable = when (val item = itemStack.item) {
                is Equipable -> item
                is BlockItem -> item.block as? Equipable
                else -> null
            } ?: return null
            
            return equipable.equipmentSlot
        }
        
    }
    
}