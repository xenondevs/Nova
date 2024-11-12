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
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.item.equipment.Equippable
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Horse
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Llama
import org.bukkit.entity.Piglin
import org.bukkit.entity.Player
import org.bukkit.entity.Skeleton
import org.bukkit.entity.Wolf
import org.bukkit.entity.Zombie
import xyz.xenondevs.commons.collections.getCoerced
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.provider.orElse
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.config.optionalEntry
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.network.ClientboundSetEquipmentPacket
import xyz.xenondevs.nova.resources.builder.task.RuntimeEquipmentData
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.util.getOrNull
import xyz.xenondevs.nova.util.getOrThrow
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.nmsEntity
import xyz.xenondevs.nova.util.nmsEquipmentSlot
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.util.toResourceLocation
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.item.equipment.Equipment
import java.util.Optional
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
fun Wearable(
    equipment: Equipment?,
    slot: BukkitEquipmentSlot,
    equipSound: Key
): ItemBehaviorFactory<Wearable> =
    Wearable(
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
fun Wearable(
    equipment: Equipment?,
    slot: BukkitEquipmentSlot, 
    equipSound: Holder<SoundEvent> = SoundEvents.ARMOR_EQUIP_GENERIC
) = ItemBehaviorFactory<Wearable> {
    val cfg = it.config
    Wearable(
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
class Wearable(
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
    
    private val equipmentData: Provider<RuntimeEquipmentData?> = ResourceLookups.EQUIPMENT_LOOKUP.getProvider(texture)
    
    private val textureFrame = mutableProvider(0)
    private val overlayFrame = mutableProvider(0)
    
    init {
        equipmentData.subscribe { equipmentData ->
            animatedWearables -= this
            if (equipmentData != null && equipmentData.isAnimated) {
                animatedWearables += this
            }
        }
    }
    
    /**
     * Provider for a 2d array of [Equippable] components, where the first index is the texture frame and the second index is the overlay frame.
     */
    private val equippableComponentFrames: Provider<Array<Array<Equippable>>> = combinedProvider(
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
                Equippable(
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
        equippableComponentFrames, textureFrame, overlayFrame, attributeModifiersComponent
    ) { equippableFrames, textureFrame, overlayFrame, attributeModifiers ->
        val equippable = equippableFrames
            .getCoerced(textureFrame)
            .getCoerced(overlayFrame)
        
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
    
    @InternalInit(stage = InternalInitStage.POST_WORLD)
    internal companion object {
        
        private val ARMOR_EQUIPMENT_SLOTS = listOf(
            BukkitEquipmentSlot.FEET,
            BukkitEquipmentSlot.LEGS, 
            BukkitEquipmentSlot.CHEST, 
            BukkitEquipmentSlot.HEAD,
            BukkitEquipmentSlot.BODY
        )
        private val animatedWearables = HashSet<Wearable>()
        
        @InitFun
        private fun startAnimationTask() {
            runTaskTimer(0, 1, ::handleTick)
        }
        
        private fun handleTick() {
            if (animatedWearables.isEmpty())
                return
            
            for (wearable in animatedWearables) {
                val textureFrames = wearable.equipmentData.get()?.textureFrames?.size ?: 1
                val overlayFrames = wearable.equipmentData.get()?.cameraOverlayFrames?.size ?: 1
                wearable.textureFrame.set((wearable.textureFrame.get() + 1) % textureFrames)
                wearable.overlayFrame.set((wearable.overlayFrame.get() + 1) % overlayFrames)
            }
            
            Bukkit.getWorlds().asSequence()
                .flatMap { it.livingEntities }
                .forEach { entity ->
                    when (entity) {
                        is Player -> updatePlayerArmor(entity)
                        
                        is HumanEntity, is Zombie, is Skeleton, is Piglin,
                        is ArmorStand, is Horse, is Llama, is Wolf -> updateNonPlayerArmor(entity)
                    }
                }
        }
        
        private fun updatePlayerArmor(player: Player) {
            val serverPlayer = player.serverPlayer
            for ((armorSlot, armorStack) in serverPlayer.inventory.armor.withIndex()) {
                var equipment = HashMap<EquipmentSlot, ItemStack>()
                if (armorStack?.novaItem?.getBehavior<Wearable>()?.equipmentData?.get()?.isAnimated == true) {
                    serverPlayer.inventoryMenu.setRemoteSlot(8 - armorSlot, ItemStack.EMPTY) // mark as dirty, force update
                    equipment[EquipmentSlot.entries[armorSlot + 2]] = armorStack
                }
                
                // update for other players
                val packet = ClientboundSetEquipmentPacket(player.entityId, equipment)
                serverPlayer.serverLevel().chunkSource.broadcast(serverPlayer, packet)
            }
        }
        
        private fun updateNonPlayerArmor(entity: LivingEntity) {
            val equipment = entity.equipment ?: return
            
            var updatedEquipment: HashMap<EquipmentSlot, ItemStack>? = null
            for (slot in ARMOR_EQUIPMENT_SLOTS) {
                if (!entity.canUseEquipmentSlot(slot))
                    continue
                
                val itemStack = equipment.getItem(slot)
                if (itemStack.novaItem?.getBehavior<Wearable>()?.equipmentData?.get()?.isAnimated == true) {
                    if (updatedEquipment == null)
                        updatedEquipment = HashMap()
                    updatedEquipment[slot.nmsEquipmentSlot] = itemStack.unwrap()
                }
            }
            
            if (updatedEquipment != null) {
                val packet = ClientboundSetEquipmentPacket(entity.entityId, updatedEquipment)
                entity.world.serverLevel.chunkSource.broadcast(entity.nmsEntity, packet)
            }
        }
        
    }
    
}