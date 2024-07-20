@file:Suppress("FunctionName")

package xyz.xenondevs.nova.item.behavior

import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Equipable
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.item.component.ItemAttributeModifiers
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.combinedProvider
import xyz.xenondevs.commons.provider.immutable.map
import xyz.xenondevs.commons.provider.immutable.orElse
import xyz.xenondevs.commons.provider.immutable.provider
import xyz.xenondevs.nova.data.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.armor.Armor
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.player.WrappedPlayerInteractEvent
import xyz.xenondevs.nova.util.bukkitEquipmentSlot
import xyz.xenondevs.nova.util.item.isActuallyInteractable
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.nmsEquipmentSlot
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.util.unwrap
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
        
        override val vanillaMaterialProperties = combinedProvider(slot, texture) { slot, texture ->
            if (texture == null)
                return@combinedProvider emptyList()
            
            return@combinedProvider listOf(
                when (slot) {
                    BukkitEquipmentSlot.HEAD -> VanillaMaterialProperty.HELMET
                    BukkitEquipmentSlot.CHEST -> VanillaMaterialProperty.CHESTPLATE
                    BukkitEquipmentSlot.LEGS -> VanillaMaterialProperty.LEGGINGS
                    BukkitEquipmentSlot.FEET -> VanillaMaterialProperty.BOOTS
                    else -> throw IllegalArgumentException("Invalid wearable slot: $slot")
                }
            )
        }
        
        override val baseDataComponents = combinedProvider(
            slot, armor, armorToughness, knockbackResistance
        ) { slot, armor, armorToughness, knockBackResistance ->
            if (armor == 0.0 && armorToughness == 0.0 && knockBackResistance == 0.0)
                return@combinedProvider DataComponentMap.EMPTY
            
            val equipmentSlot = slot.nmsEquipmentSlot
            DataComponentMap.builder().set(
                DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.builder().apply {
                    if (armor != 0.0) {
                        add(
                            Attributes.ARMOR,
                            AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath("nova", "armor"),
                                armor,
                                Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.bySlot(equipmentSlot)
                        )
                    }
                    
                    if (armorToughness != 0.0) {
                        add(
                            Attributes.ARMOR_TOUGHNESS,
                            AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath("nova", "armor_toughness"),
                                armorToughness,
                                Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.bySlot(equipmentSlot)
                        )
                    }
                    
                    if (knockBackResistance != 0.0) {
                        add(
                            Attributes.KNOCKBACK_RESISTANCE,
                            AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath("nova", "knockback_resistance"),
                                knockBackResistance,
                                Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.bySlot(equipmentSlot)
                        )
                    }
                }.build()
            ).build()
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
                player.serverPlayer.onEquipItem(slot.nmsEquipmentSlot, previous.unwrap().copy(), itemStack.unwrap().copy())
                wrappedEvent.actionPerformed = true
            } else {
                // basically marks the remote armor slot as dirty, see https://hub.spigotmc.org/jira/browse/SPIGOT-7500
                player.serverPlayer.inventoryMenu.setRemoteSlot(slot.inventorySlot, itemStack.unwrap().copy())
            }
        }
        
        override fun modifyClientSideStack(player: Player?, itemStack: ItemStack, data: NamespacedCompound): ItemStack {
            val texture = texture
            if (texture != null) {
                itemStack.unwrap().set(DataComponents.DYED_COLOR, DyedItemColor(texture, false))
            }
            
            return itemStack
        }
        
    }
    
    companion object {
        
        /**
         * Checks whether the specified [itemStack] is wearable.
         */
        fun isWearable(itemStack: BukkitStack): Boolean =
            isWearable(itemStack.unwrap().copy())
        
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
            getSlot(itemStack.unwrap().copy())?.bukkitEquipmentSlot
        
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