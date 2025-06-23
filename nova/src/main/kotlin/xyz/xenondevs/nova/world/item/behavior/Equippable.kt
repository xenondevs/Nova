@file:Suppress("FunctionName")

package xyz.xenondevs.nova.world.item.behavior

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Equippable.equippable
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers.itemAttributes
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.keys.SoundEventKeys
import io.papermc.paper.registry.set.RegistryKeySet
import io.papermc.paper.registry.set.RegistrySet
import net.kyori.adventure.key.Key
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.attribute.AttributeModifier.Operation
import org.bukkit.entity.EntityType
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.collections.getMod
import xyz.xenondevs.commons.collections.takeUnlessEmpty
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.orElse
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.config.entryOrElse
import xyz.xenondevs.nova.config.optionalEntry
import xyz.xenondevs.nova.resources.builder.task.RuntimeEquipmentData
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.world.item.DataComponentMap
import xyz.xenondevs.nova.world.item.Equipment
import xyz.xenondevs.nova.world.item.buildDataComponentMapProvider

/**
 * Creates a factory for [Equippable] behaviors using the given values, if not specified in the item's configuration.
 *
 * @param equipment The equipment texture to use, or null for no texture.
 *
 * @param slot The slot in which the item can be worn.
 *
 * @param armor The amount of armor this item provides. Defaults to `0`.
 * Used when `armor` is not specified in the config.
 *
 * @param armorToughness The amount of armor toughness this item provides. Defaults to `0`.
 * Used when `armor_toughness` is not specified in the config.
 *
 * @param knockbackResistance The amount of knockback resistance this item provides. Defaults to `0`.
 * Used when `knockback_resistance` is not specified in the config.
 *
 * @param equipSound The sound that is played when the item is equipped. Defaults to `item.armor.equip_generic`.
 * Used when `equip_sound` is not specified in the config.
 *
 * @param allowedEntities The entity types that are allowed to wear this item, or null for all entities.
 * Used when `allowed_entities` is not specified in the config.
 *
 * @param dispensable Whether this item can be dispensed from a dispenser. Defaults to `true`.
 * Used when `dispensable` is not specified in the config.
 *
 * @param swappable Whether this item can be swapped with other items in the same slot. Defaults to `true`.
 * Used when `swappable` is not specified in the config.
 *
 * @param damageOnHurt Whether this item will be damaged when the wearing entity is damaged. Defaults to `true`
 * Used when `damage_on_hurt` is not specified in the config.
 *
 * @param equipOnInteract Whether this item should be equipped when right-clicking. Defaults to `true`.
 * Used when `equip_on_interact` is not specified in the config.
 *
 * @param canBeSheared Whether players can use shears to remove this equippable item from a wearing entity. Defaults to `false`.
 * Used when `can_be_sheared` is not specified in the config.
 *
 * @param shearingSound The sound that is played when the equippable is removed by shears. Defaults to `item.shears.snip`.
 * Used when `shearing_sound` is not specified in the config.
 */
fun Equippable(
    equipment: Equipment?,
    slot: EquipmentSlot,
    armor: Double = 0.0,
    armorToughness: Double = 0.0,
    knockbackResistance: Double = 0.0,
    equipSound: Key = SoundEventKeys.ITEM_ARMOR_EQUIP_GENERIC,
    allowedEntities: Set<EntityType>?,
    dispensable: Boolean = true,
    swappable: Boolean = true,
    damageOnHurt: Boolean = true,
    equipOnInteract: Boolean = true,
    canBeSheared: Boolean = false,
    shearingSound: Key = SoundEventKeys.ITEM_SHEARS_SNIP
): ItemBehaviorFactory<Equippable> = Equippable(
    equipment,
    slot,
    armor,
    armorToughness,
    knockbackResistance,
    equipSound,
    allowedEntities?.let { RegistrySet.keySetFromValues(RegistryKey.ENTITY_TYPE, it) },
    dispensable,
    swappable,
    damageOnHurt,
    equipOnInteract,
    canBeSheared,
    shearingSound
)

/**
 * Creates a factory for [Equippable] behaviors using the given values, if not specified in the item's configuration.
 *
 * @param equipment The equipment texture to use, or null for no texture.
 *
 * @param slot The slot in which the item can be worn.
 *
 * @param armor The amount of armor this item provides. Defaults to `0`.
 * Used when `armor` is not specified in the config.
 *
 * @param armorToughness The amount of armor toughness this item provides. Defaults to `0`.
 * Used when `armor_toughness` is not specified in the config.
 *
 * @param knockbackResistance The amount of knockback resistance this item provides. Defaults to `0`.
 * Used when `knockback_resistance` is not specified in the config.
 *
 * @param equipSound The sound that is played when the item is equipped. Defaults to `item.armor.equip_generic`.
 * Used when `equip_sound` is not specified in the config.
 *
 * @param allowedEntities The entity types that are allowed to wear this item, or null for all entities.
 * Used when `allowed_entities` is not specified in the config.
 *
 * @param dispensable Whether this item can be dispensed from a dispenser. Defaults to `true`.
 * Used when `dispensable` is not specified in the config.
 *
 * @param swappable Whether this item can be swapped with other items in the same slot. Defaults to `true`.
 * Used when `swappable` is not specified in the config.
 *
 * @param damageOnHurt Whether this item will be damaged when the wearing entity is damaged. Defaults to `true`
 * Used when `damage_on_hurt` is not specified in the config.
 *
 * @param equipOnInteract Whether this item should be equipped when right-clicking. Defaults to `true`.
 * Used when `equip_on_interact` is not specified in the config.
 *
 * @param canBeSheared Whether players can use shears to remove this equippable item from a wearing entity. Defaults to `false`.
 * Used when `can_be_sheared` is not specified in the config.
 *
 * @param shearingSound The sound that is played when the equippable is removed by shears. Defaults to `item.shears.snip`.
 * Used when `shearing_sound` is not specified in the config.
 */
fun Equippable(
    equipment: Equipment?,
    slot: EquipmentSlot,
    armor: Double = 0.0,
    armorToughness: Double = 0.0,
    knockbackResistance: Double = 0.0,
    equipSound: Key = SoundEventKeys.ITEM_ARMOR_EQUIP_GENERIC,
    allowedEntities: RegistryKeySet<EntityType>? = null,
    dispensable: Boolean = true,
    swappable: Boolean = true,
    damageOnHurt: Boolean = true,
    equipOnInteract: Boolean = true,
    canBeSheared: Boolean = false,
    shearingSound: Key = SoundEventKeys.ITEM_SHEARS_SNIP
) = ItemBehaviorFactory<Equippable> {
    val cfg = it.config
    Equippable(
        provider(equipment),
        provider(slot),
        cfg.entryOrElse(armor, "armor"),
        cfg.entryOrElse(armorToughness, "armor_toughness"),
        cfg.entryOrElse(knockbackResistance, "knockback_resistance"),
        cfg.entryOrElse(equipSound, "equip_sound"),
        cfg.optionalEntry<RegistryKeySet<EntityType>>("allowed_entities").orElse(allowedEntities),
        cfg.entryOrElse(dispensable, "dispensable"),
        cfg.entryOrElse(swappable, "swappable"),
        cfg.entryOrElse(damageOnHurt, "damage_on_hurt"),
        cfg.entryOrElse(equipOnInteract, "equip_on_interact"),
        cfg.entryOrElse(canBeSheared, "can_be_sheared"),
        cfg.entryOrElse(shearingSound, "shearing_sound")
    )
}

/**
 * Allows items to be worn in armor slots.
 *
 * @param equipment The equipment texture to use, or null for no texture.
 * @param slot The slot in which the item can be worn.
 * @param armor The amount of armor this item provides.
 * @param armorToughness The amount of armor toughness this item provides.
 * @param knockbackResistance The amount of knockback resistance this item provides.
 * @param equipSound The sound that is played when the item is equipped.
 * @param allowedEntities The entity types that are allowed to wear this item, or null for all entities.
 * @param dispensable Whether this item can be dispensed from a dispenser.
 * @param swappable Whether this item can be swapped with other items in the same slot.
 * @param damageOnHurt Whether this item will be damaged when the wearing entity is damaged.
 * @param equipOnInteract Whether this item should be equipped when right-clicking.
 * @param canBeSheared Whether players can use shears to remove this equippable item from a wearing entity.
 * @param shearingSound The sound that is played when the equippable is removed by shears.
 */
class Equippable(
    equipment: Provider<Equipment?>,
    slot: Provider<EquipmentSlot>,
    armor: Provider<Double>,
    armorToughness: Provider<Double>,
    knockbackResistance: Provider<Double>,
    equipSound: Provider<Key>,
    allowedEntities: Provider<RegistryKeySet<EntityType>?>,
    dispensable: Provider<Boolean>,
    swappable: Provider<Boolean>,
    damageOnHurt: Provider<Boolean>,
    equipOnInteract: Provider<Boolean>,
    canBeSheared: Provider<Boolean>,
    shearingSound: Provider<Key>
) : ItemBehavior {
    
    /**
     * The custom armor texture, or null if no custom texture should be used.
     */
    val texture by equipment
    
    /**
     * The slot in which the item can be worn.
     */
    val slot: EquipmentSlot by slot
    
    /**
     * The amount of armor this item provides.
     */
    val armor: Double by armor
    
    /**
     * The amount of armor toughness this item provides.
     */
    val armorToughness: Double by armorToughness
    
    /**
     * The amount of knockback resistance this item provides.
     */
    val knockbackResistance: Double by knockbackResistance
    
    /**
     * The key of the sound that is played when the item is equipped.
     */
    val equipSoundKey: Key by equipSound
    
    /**
     * The sound that is played when the item is equipped.
     */
    val equipSound: Sound?
        get() = Registry.SOUND_EVENT.get(equipSoundKey)
    
    /**
     * The ids of the entities that are allowed to wear this item.
     */
    val allowedEntityKeys: RegistryKeySet<EntityType>? by allowedEntities
    
    /**
     * The entity types that are allowed to wear this item.
     */
    val allowedEntities: Set<EntityType?>?
        get() = allowedEntityKeys?.mapTo(HashSet(), Registry.ENTITY_TYPE::get)
    
    /**
     * Whether this item can be dispensed from a dispenser.
     */
    val dispensable: Boolean by dispensable
    
    /**
     * Whether this item can be swapped with other items in the same slot.
     */
    val swappable: Boolean by swappable
    
    /**
     * Whether this item will be damaged when the wearing entity is damaged.
     */
    val damageOnHurt: Boolean by damageOnHurt
    
    /**
     * Whether this item should be equipped when right-clicking.
     */
    val equipOnInteract: Boolean by equipOnInteract
    
    /**
     * Whether players can use shears to remove this equippable item from a wearing entity.
     */
    val canBeSheared: Boolean by canBeSheared
    
    /**
     * The sound that is played when the equippable is removed by shears.
     */
    val shearingSoundKey: Key by shearingSound
    
    /**
     * The sound that is played when the equippable is removed by shears.
     */
    val shearingSound: Sound?
        get() = Registry.SOUND_EVENT.get(shearingSoundKey)
    
    private val equipmentData: Provider<RuntimeEquipmentData?> = ResourceLookups.EQUIPMENT_LOOKUP.getProvider(equipment)
    
    init {
        equipmentData.subscribe { equipmentData ->
            EquipmentAnimator.animatedBehaviors -= this
            if (equipmentData != null && equipmentData.isAnimated) {
                EquipmentAnimator.animatedBehaviors += this
            }
        }
    }
    
    override val baseDataComponents: Provider<DataComponentMap> = buildDataComponentMapProvider {
        this[DataComponentTypes.ATTRIBUTE_MODIFIERS] = combinedProvider(
            slot, armor, armorToughness, knockbackResistance
        ) { slot, armor, armorToughness, knockbackResistance ->
            val slotGroup = when (slot) {
                EquipmentSlot.HAND -> EquipmentSlotGroup.MAINHAND
                EquipmentSlot.OFF_HAND -> EquipmentSlotGroup.OFFHAND
                EquipmentSlot.FEET -> EquipmentSlotGroup.FEET
                EquipmentSlot.LEGS -> EquipmentSlotGroup.LEGS
                EquipmentSlot.CHEST -> EquipmentSlotGroup.CHEST
                EquipmentSlot.HEAD -> EquipmentSlotGroup.HEAD
                EquipmentSlot.BODY -> EquipmentSlotGroup.BODY
                EquipmentSlot.SADDLE -> EquipmentSlotGroup.SADDLE
            }
            
            val builder = itemAttributes()
            if (armor != 0.0) {
                builder.addModifier(
                    Attribute.ARMOR,
                    AttributeModifier(
                        NamespacedKey("nova", "armor_${slot.name.lowercase()}"),
                        armor,
                        Operation.ADD_NUMBER
                    ),
                    slotGroup
                )
            }
            
            if (armorToughness != 0.0) {
                builder.addModifier(
                    Attribute.ARMOR_TOUGHNESS,
                    AttributeModifier(
                        NamespacedKey("nova", "armor_toughness_${slot.name.lowercase()}"),
                        armorToughness,
                        Operation.ADD_NUMBER
                    ),
                    slotGroup
                )
            }
            
            if (knockbackResistance != 0.0) {
                builder.addModifier(
                    Attribute.KNOCKBACK_RESISTANCE,
                    AttributeModifier(
                        NamespacedKey("nova", "knockback_resistance_${slot.name.lowercase()}"),
                        knockbackResistance,
                        Operation.ADD_NUMBER
                    ),
                    slotGroup
                )
            }
            
            builder.build()
        }
        
        this[DataComponentTypes.EQUIPPABLE] = combinedProvider(
            equipment, equipmentData, slot, equipSound, allowedEntities, dispensable, swappable, damageOnHurt, equipOnInteract, EquipmentAnimator.tick
        ) { equipment, equipmentData, slot, equipSound, allowedEntities, dispensable, swappable, damageOnHurt, equipOnInteract, tick ->
            val textureFrames = equipmentData?.textureFrames
                ?.takeUnlessEmpty()
                ?: equipment?.id?.let(::listOf)
            val overlayFrames = equipmentData?.cameraOverlayFrames
                ?.takeUnlessEmpty()
                ?.takeUnless { slot != EquipmentSlot.HEAD }
            
            equippable(slot)
                .equipSound(equipSound)
                .assetId(textureFrames?.getMod(tick))
                .cameraOverlay(overlayFrames?.getMod(tick))
                .allowedEntities(allowedEntities)
                .dispensable(dispensable)
                .swappable(swappable)
                .damageOnHurt(damageOnHurt)
                .equipOnInteract(equipOnInteract)
                .build()
        }
    }
    
    override fun toString(itemStack: ItemStack): String {
        return "Equippable(" +
            "slot=$slot, " +
            "armor=$armor, " +
            "armorToughness=$armorToughness, " +
            "knockbackResistance=$knockbackResistance" +
            "equipSound=$equipSound, " +
            "allowedEntities=$allowedEntities, " +
            "dispensable=$dispensable, " +
            "swappable=$swappable, " +
            "damageOnHurt=$damageOnHurt, " +
            "equipOnInteract=$equipOnInteract, " +
            "canBeSheared=$canBeSheared, " +
            "shearingSound=$shearingSound" +
            ")"
    }
    
}