@file:Suppress("FunctionName")

package xyz.xenondevs.nova.world.item.behavior

import net.kyori.adventure.key.Key
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.component.ItemAttributeModifiers
import org.bukkit.Sound
import xyz.xenondevs.commons.collections.getMod
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.orElse
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.config.optionalEntry
import xyz.xenondevs.nova.resources.builder.task.RuntimeEquipmentData
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.util.getOrNull
import xyz.xenondevs.nova.util.getOrThrow
import xyz.xenondevs.nova.util.nmsEquipmentSlot
import xyz.xenondevs.nova.util.toResourceLocation
import xyz.xenondevs.nova.world.item.equipment.Equipment
import java.util.Optional
import net.minecraft.world.item.equipment.Equippable as EquippableComponent
import org.bukkit.entity.EntityType as BukkitEntityType
import org.bukkit.inventory.EquipmentSlot as BukkitEquipmentSlot
import org.bukkit.inventory.ItemStack as BukkitStack

/**
 * Allows items to be worn in armor slots.
 *
 * @param equipment The custom armor texture, or null if no custom texture should be used.
 * @param slot The slot in which the item can be worn.
 * @param equipSound The sound that is played when the item is equipped.
 */
fun Equippable(
    equipment: Equipment?,
    slot: BukkitEquipmentSlot,
    equipSound: Key
): ItemBehaviorFactory<Equippable> =
    Equippable(
        equipment, slot,
        BuiltInRegistries.SOUND_EVENT.getOrNull(equipSound)
            ?: Holder.direct(SoundEvent(equipSound.toResourceLocation(), Optional.empty()))
    )

/**
 * Allows items to be worn in armor slots.
 *
 * @param equipment The custom armor texture, or null if no custom texture should be used.
 * @param slot The slot in which the item can be worn.
 * @param equipSound The sound that is played when the item is equipped.
 */
fun Equippable(
    equipment: Equipment?,
    slot: BukkitEquipmentSlot,
    equipSound: Holder<SoundEvent> = SoundEvents.ARMOR_EQUIP_GENERIC
) = ItemBehaviorFactory<Equippable> {
    val cfg = it.config
    Equippable(
        provider(equipment),
        provider(slot),
        cfg.optionalEntry<Double>("armor").orElse(0.0),
        cfg.optionalEntry<Double>("armor_toughness").orElse(0.0),
        cfg.optionalEntry<Double>("knockback_resistance").orElse(0.0),
        provider(equipSound),
        cfg.optionalEntry<Set<BukkitEntityType>>("allowed_entities"),
        cfg.optionalEntry<Boolean>("dispensable").orElse(true),
        cfg.optionalEntry<Boolean>("swappable").orElse(true),
        cfg.optionalEntry<Boolean>("damage_on_hurt").orElse(true)
    )
}

/**
 * Allows items to be worn in armor slots.
 *
 * @param texture The leather armor color used for the custom texture, or null if no custom texture should be used.
 * @param slot The slot in which the item can be worn.
 * @param armor The amount of armor this item provides.
 * @param armorToughness The amount of armor toughness this item provides.
 * @param knockbackResistance The amount of knockback resistance this item provides.
 * @param equipSound The sound that is played when the item is equipped, or null if no sound should be played.
 */
class Equippable(
    texture: Provider<Equipment?>,
    slot: Provider<BukkitEquipmentSlot>,
    armor: Provider<Double>,
    armorToughness: Provider<Double>,
    knockbackResistance: Provider<Double>,
    equipSound: Provider<Holder<SoundEvent>>,
    allowedEntities: Provider<Set<BukkitEntityType>?>,
    dispensable: Provider<Boolean>,
    swappable: Provider<Boolean>,
    damageOnHurt: Provider<Boolean>,
) : ItemBehavior {
    
    constructor(
        texture: Equipment?,
        slot: BukkitEquipmentSlot,
        armor: Double,
        armorToughness: Double,
        knockbackResistance: Double,
        equipSound: Sound,
        allowedEntities: Set<BukkitEntityType>,
        dispensable: Boolean,
        swappable: Boolean,
        damageOnHurt: Boolean,
    ) : this(
        provider(texture),
        provider(slot),
        provider(armor),
        provider(armorToughness),
        provider(knockbackResistance),
        provider { BuiltInRegistries.SOUND_EVENT.getOrThrow(equipSound.key()) },
        provider(allowedEntities),
        provider(dispensable),
        provider(swappable),
        provider(damageOnHurt),
    )
    
    /**
     * The custom armor texture, or null if no custom texture should be used.
     */
    val texture by texture
    
    /**
     * The slot in which the item can be worn.
     */
    val slot: BukkitEquipmentSlot by slot
    
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
     * The sound that is played when the item is equipped.
     */
    val equipSound: Holder<SoundEvent> by equipSound
    
    /**
     * The [entity types][BukkitEntityType] that are allowed to wear this item.
     */
    val allowedEntities: Set<BukkitEntityType>? by allowedEntities
    
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
    
    internal val equipmentData: Provider<RuntimeEquipmentData?> = ResourceLookups.EQUIPMENT_LOOKUP.getProvider(texture)
    
    init {
        equipmentData.subscribe { equipmentData ->
            EquipmentAnimator.animatedBehaviors -= this
            if (equipmentData != null && equipmentData.isAnimated) {
                EquipmentAnimator.animatedBehaviors += this
            }
        }
    }
    
    /**
     * Provider for a 2d array of [EquippableComponent] components, where the first index is the texture frame and the second index is the overlay frame.
     */
    private val equippableComponentFrames: Provider<Array<Array<EquippableComponent>>> = combinedProvider(
        equipmentData, slot, equipSound, allowedEntities, dispensable, swappable, damageOnHurt
    ) { equipmentData, slot, equipSound, allowedEntities, dispensable, swappable, damageOnHurt ->
        val equipmentSlot = slot.nmsEquipmentSlot
        
        val textureFrames = equipmentData?.textureFrames
        val overlayFrames = equipmentData?.cameraOverlayFrames
            ?.takeUnless { slot != BukkitEquipmentSlot.HEAD }
        
        val allowedNmsEntities = allowedEntities
            ?.map { BuiltInRegistries.ENTITY_TYPE.getOrThrow(it.key()) }
            ?.let { HolderSet.direct(it) as HolderSet<EntityType<*>> }
        
        Array(textureFrames?.size ?: 1) { textureFrame ->
            Array(overlayFrames?.size ?: 1) { overlayFrame ->
                EquippableComponent(
                    equipmentSlot,
                    equipSound,
                    Optional.ofNullable(textureFrames?.getOrNull(textureFrame)),
                    Optional.ofNullable(overlayFrames?.getOrNull(overlayFrame)),
                    Optional.ofNullable(allowedNmsEntities),
                    dispensable, swappable, damageOnHurt
                )
            }
        }
    }
    
    /**
     * Provider for the attribute modifiers.
     */
    private val attributeModifiersComponent: Provider<ItemAttributeModifiers> = combinedProvider(
        slot, armor, armorToughness, knockbackResistance
    ) { slot, armor, armorToughness, knockbackResistance ->
        val equipmentSlot = slot.nmsEquipmentSlot
        val builder = ItemAttributeModifiers.builder()
        if (armor != 0.0) {
            builder.add(
                Attributes.ARMOR,
                AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath("nova", "armor_${slot.name.lowercase()}"),
                    armor,
                    Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.bySlot(equipmentSlot)
            )
        }
        
        if (armorToughness != 0.0) {
            builder.add(
                Attributes.ARMOR_TOUGHNESS,
                AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath("nova", "armor_toughness_${slot.name.lowercase()}"),
                    armorToughness,
                    Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.bySlot(equipmentSlot)
            )
        }
        
        if (knockbackResistance != 0.0) {
            builder.add(
                Attributes.KNOCKBACK_RESISTANCE,
                AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath("nova", "knockback_resistance_${slot.name.lowercase()}"),
                    knockbackResistance,
                    Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.bySlot(equipmentSlot)
            )
        }
        
        return@combinedProvider builder.build()
    }
    
    override val baseDataComponents = combinedProvider(
        equippableComponentFrames, EquipmentAnimator.tick, attributeModifiersComponent
    ) { equippableFrames, tick, attributeModifiers ->
        val equippable = equippableFrames.getMod(tick).getMod(tick)
        DataComponentMap.builder()
            .set(DataComponents.EQUIPPABLE, equippable)
            .set(DataComponents.ATTRIBUTE_MODIFIERS, attributeModifiers)
            .build()
    }
    
    override fun toString(itemStack: BukkitStack): String {
        return "Wearable(" +
            "slot=$slot, " +
            "armor=$armor, " +
            "armorToughness=$armorToughness, " +
            "knockbackResistance=$knockbackResistance" +
            ")"
    }
    
}