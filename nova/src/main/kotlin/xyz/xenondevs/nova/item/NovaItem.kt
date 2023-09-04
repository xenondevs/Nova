@file:Suppress("unused", "MemberVisibilityCanBePrivate", "UNCHECKED_CAST")

package xyz.xenondevs.nova.item

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.ai.attributes.Attribute
import org.bukkit.Material
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
import org.spongepowered.configurate.kotlin.extensions.get
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.nmsutils.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.ConfigProvider
import xyz.xenondevs.nova.data.config.Configs
import xyz.xenondevs.nova.data.config.Reloadable
import xyz.xenondevs.nova.data.resources.builder.task.material.info.VanillaMaterialTypes
import xyz.xenondevs.nova.data.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.data.resources.model.data.ItemModelData
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.item.behavior.ItemBehaviorFactory
import xyz.xenondevs.nova.item.behavior.ItemBehaviorHolder
import xyz.xenondevs.nova.item.logic.PacketItemData
import xyz.xenondevs.nova.item.vanilla.AttributeModifier
import xyz.xenondevs.nova.player.equipment.ArmorEquipEvent
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.data.CentralizedLazy
import xyz.xenondevs.nova.util.data.LazyArray
import xyz.xenondevs.nova.util.data.logExceptionMessages
import xyz.xenondevs.nova.util.item.novaCompound
import xyz.xenondevs.nova.util.item.novaCompoundOrNull
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.event.BlockBreakActionEvent
import java.util.logging.Level
import kotlin.math.min
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf
import net.minecraft.world.entity.EquipmentSlot as MojangEquipmentSlot
import net.minecraft.world.item.ItemStack as MojangStack

/**
 * Represents an item type in Nova.
 */
class NovaItem internal constructor(
    val id: ResourceLocation,
    val name: Component,
    val style: Style,
    behaviorHolders: List<ItemBehaviorHolder>,
    private val _maxStackSize: Int = 64,
    val craftingRemainingItem: ItemBuilder? = null,
    val isHidden: Boolean = false,
    val block: NovaBlock? = null,
    private val configId: String = id.toString()
) : Reloadable {
    
    /**
     * The maximum stack size of this [NovaItem].
     */
    val maxStackSize: Int
        get() = min(_maxStackSize, vanillaMaterial.maxStackSize)
    
    /**
     * The [ItemModelData] containing all the vanilla material and custom model data to be used for this [NovaItem].
     */
    val model: ItemModelData by lazy {
        val itemModelData = ResourceLookups.MODEL_DATA_LOOKUP.getOrThrow(id).item!!
        if (itemModelData.size == 1)
            return@lazy itemModelData.values.first()
        
        return@lazy itemModelData[vanillaMaterial]!!
    }
    
    /**
     * An array of [ItemProviders][ItemProvider] for each subId of this [NovaItem].
     *
     * The items are in client-side format and do not have any other special data except their display name (hence "basic").
     */
    val basicClientsideProviders: LazyArray<ItemProvider> =
        LazyArray({ model.dataArray.size }) { model.createClientsideItemProvider(this, true, it) }
    
    /**
     * An array of [ItemProviders][ItemProvider] for each subId of this [NovaItem].
     *
     * The items are in client-side format and have all special data (lore, other nbt tags, etc.) applied.
     */
    val clientsideProviders: LazyArray<ItemProvider> =
        LazyArray({ model.dataArray.size }) { model.createClientsideItemProvider(this, false, it) }
    
    /**
     * The basic client-side provider for the first subId of this [NovaItem].
     * @see [basicClientsideProviders]
     */
    val basicClientsideProvider: ItemProvider by lazy { basicClientsideProviders[0] }
    
    /**
     * The client-side provider for the first subId of this [NovaItem].
     * @see [clientsideProviders]
     */
    val clientsideProvider: ItemProvider by lazy { clientsideProviders[0] }
    
    /**
     * The configuration for this [NovaItem].
     * Trying to read config values from this when no config is present will result in an exception.
     *
     * Use the extension functions `entry` and `optionalEntry` to get values from the config.
     */
    val config: ConfigProvider by lazy { Configs[configId] }
    
    /**
     * The [ItemBehaviors][ItemBehavior] of this [NovaItem].
     */
    val behaviors: List<ItemBehavior> = behaviorHolders.map { holder ->
        when (holder) {
            is ItemBehavior -> holder
            is ItemBehaviorFactory<*> -> holder.create(this)
        }
    }
    
    /**
     * The underlying vanilla material of this [NovaItem].
     */
    internal var vanillaMaterial: Material by CentralizedLazy(::reload)
        private set
    
    /**
     * The attribute modifiers that are applied when this [NovaItem] is equipped.
     */
    internal var attributeModifiers: Map<MojangEquipmentSlot, List<AttributeModifier>> by CentralizedLazy(::reload)
        private set
    
    /**
     * The default [NamespacedCompound] that is applied to all [ItemStacks][ItemStack] of this [NovaItem].
     */
    private var defaultCompound: NamespacedCompound? by CentralizedLazy(::reload)
    
    /**
     * Creates an [ItemBuilder] for an [ItemStack] of this [NovaItem], in server-side format.
     */
    fun createItemBuilder(): ItemBuilder =
        modifyItemBuilder(model.createItemBuilder())
    
    /**
     * Creates an [ItemStack] of this [NovaItem] in server-side format.
     *
     * Functionally equivalent to: `createItemBuilder().setAmount(amount).get()`
     */
    fun createItemStack(amount: Int = 1): ItemStack =
        createItemBuilder().setAmount(amount).get()
    
    /**
     * Creates an [ItemBuilder] for an [ItemStack] of this [NovaItem] in client-side format.
     */
    fun createClientsideItemBuilder(): ItemBuilder =
        model.createClientsideItemBuilder()
    
    /**
     * Creates an [ItemStack] of this [NovaItem] in client-side format.
     *
     * Functionally equivalent to: `createClientsideItemBuilder().setAmount(amount).get()`
     */
    fun createClientsideItemStack(amount: Int): ItemStack =
        createClientsideItemBuilder().setAmount(amount).get()
    
    /**
     * Checks whether this [NovaItem] has an [ItemBehavior] of the reified type [T], or a subclass of it.
     */
    inline fun <reified T : Any> hasBehavior(): Boolean =
        hasBehavior(T::class)
    
    /**
     * Checks whether this [NovaItem] has an [ItemBehavior] of the specified class [behavior], or a subclass of it.
     */
    fun <T : Any> hasBehavior(type: KClass<T>): Boolean =
        behaviors.any { it::class == type }
    
    /**
     * Gets the first [ItemBehavior] that is an instance of [T], or null if there is none.
     */
    inline fun <reified T : Any> getBehaviorOrNull(): T? =
        getBehaviorOrNull(T::class)
    
    /**
     * Gets the first [ItemBehavior] that is an instance of [behavior], or null if there is none.
     */
    fun <T : Any> getBehaviorOrNull(type: KClass<T>): T? =
        behaviors.firstOrNull { type.isSuperclassOf(it::class) } as T?
    
    /**
     * Gets the first [ItemBehavior] that is an instance of [T], or throws an [IllegalStateException] if there is none.
     */
    inline fun <reified T : Any> getBehavior(): T =
        getBehavior(T::class)
    
    /**
     * Gets the first [ItemBehavior] that is an instance of [behavior], or throws an [IllegalStateException] if there is none.
     */
    fun <T : Any> getBehavior(behavior: KClass<T>): T =
        getBehaviorOrNull(behavior) ?: throw IllegalStateException("Item $id does not have a behavior of type ${behavior.simpleName}")
    
    @Suppress("DEPRECATION")
    internal fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
        var builder = itemBuilder
        if (defaultCompound != null)
            builder.addModifier { it.novaCompound.putAll(defaultCompound!!.copy()); it }
        behaviors.forEach { builder = it.modifyItemBuilder(builder) }
        return builder
    }
    
    internal fun getPacketItemData(itemStack: MojangStack?): PacketItemData {
        val itemData = PacketItemData(itemStack?.orCreateTag ?: CompoundTag(), name)
        behaviors.forEach { it.updatePacketItemData(itemStack?.novaCompoundOrNull ?: NamespacedCompound.EMPTY, itemData) }
        return itemData
    }
    
    //<editor-fold desc="load methods", defaultstate="collapsed">
    override fun reload() {
        loadVanillaMaterial()
        loadAttributeModifiers()
        loadDefaultCompound()
    }
    
    private fun loadVanillaMaterial() {
        val properties = behaviors.flatMapTo(HashSet()) { it.getVanillaMaterialProperties() }
        vanillaMaterial = VanillaMaterialTypes.getMaterial(properties)
    }
    
    private fun loadAttributeModifiers() {
        val modifiers = loadConfiguredAttributeModifiers() + behaviors.flatMap { it.getAttributeModifiers() }
        val modifiersBySlot = enumMap<MojangEquipmentSlot, ArrayList<AttributeModifier>>()
        modifiers.forEach { modifier ->
            modifier.slots.forEach { slot ->
                modifiersBySlot.getOrPut(slot, ::ArrayList) += modifier
            }
        }
        attributeModifiers = modifiersBySlot
    }
    
    private fun loadConfiguredAttributeModifiers(): List<AttributeModifier> {
        val section = Configs.getOrNull(configId)?.node("attribute_modifiers")
        if (section == null || section.virtual())
            return emptyList()
        
        val modifiers = ArrayList<AttributeModifier>()
        for ((slotName, attributesNode) in section.childrenMap()) {
            try {
                val slot = MojangEquipmentSlot.entries.firstOrNull { it.name.equals(slotName as String, true) }
                    ?: throw IllegalArgumentException("Unknown equipment slot: $slotName")
                
                for ((idx, attributeNode) in attributesNode.childrenList().withIndex()) {
                    try {
                        val attribute = attributeNode.node("attribute").get<Attribute>()
                            ?: throw NoSuchElementException("Missing value 'attribute'")
                        val operation = attributeNode.node("operation").get<net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation>()
                            ?: throw NoSuchElementException("Missing value 'operation'")
                        val value = attributeNode.node("value").get<Double>()
                            ?: throw NoSuchElementException("Missing value 'value'")
                        val hidden = attributeNode.node("hidden").boolean
                        
                        modifiers += AttributeModifier(
                            "Nova Configured Attribute Modifier ($slot, $idx)",
                            attribute,
                            operation,
                            value,
                            !hidden,
                            slot
                        )
                    } catch (e: Exception) {
                        LOGGER.logExceptionMessages(Level.WARNING, "Failed to load attribute modifier for $this, $slot with index $idx", e)
                    }
                }
            } catch (e: Exception) {
                LOGGER.logExceptionMessages(Level.WARNING, "Failed to load attribute modifier for $this", e)
            }
        }
        
        return modifiers
    }
    
    private fun loadDefaultCompound() {
        var defaultCompound: NamespacedCompound? = null
        for (behavior in behaviors) {
            val behaviorCompound = behavior.getDefaultCompound()
            if (behaviorCompound.isNotEmpty()) {
                if (defaultCompound == null)
                    defaultCompound = NamespacedCompound()
                
                defaultCompound.putAll(behaviorCompound)
            }
        }
        this.defaultCompound = defaultCompound
    }
    //</editor-fold>
    
    //<editor-fold desc="event methods", defaultstate="collapsed">
    internal fun handleInteract(player: Player, itemStack: ItemStack, action: Action, event: PlayerInteractEvent) {
        behaviors.forEach { it.handleInteract(player, itemStack, action, event) }
    }
    
    internal fun handleEntityInteract(player: Player, itemStack: ItemStack, clicked: Entity, event: PlayerInteractAtEntityEvent) {
        behaviors.forEach { it.handleEntityInteract(player, itemStack, clicked, event) }
    }
    
    internal fun handleAttackEntity(player: Player, itemStack: ItemStack, attacked: Entity, event: EntityDamageByEntityEvent) {
        behaviors.forEach { it.handleAttackEntity(player, itemStack, attacked, event) }
    }
    
    internal fun handleBreakBlock(player: Player, itemStack: ItemStack, event: BlockBreakEvent) {
        behaviors.forEach { it.handleBreakBlock(player, itemStack, event) }
    }
    
    internal fun handleDamage(player: Player, itemStack: ItemStack, event: PlayerItemDamageEvent) {
        behaviors.forEach { it.handleDamage(player, itemStack, event) }
    }
    
    internal fun handleBreak(player: Player, itemStack: ItemStack, event: PlayerItemBreakEvent) {
        behaviors.forEach { it.handleBreak(player, itemStack, event) }
    }
    
    internal fun handleEquip(player: Player, itemStack: ItemStack, equipped: Boolean, event: ArmorEquipEvent) {
        behaviors.forEach { it.handleEquip(player, itemStack, equipped, event) }
    }
    
    internal fun handleInventoryClick(player: Player, itemStack: ItemStack, event: InventoryClickEvent) {
        behaviors.forEach { it.handleInventoryClick(player, itemStack, event) }
    }
    
    internal fun handleInventoryClickOnCursor(player: Player, itemStack: ItemStack, event: InventoryClickEvent) {
        behaviors.forEach { it.handleInventoryClickOnCursor(player, itemStack, event) }
    }
    
    internal fun handleInventoryHotbarSwap(player: Player, itemStack: ItemStack, event: InventoryClickEvent) {
        behaviors.forEach { it.handleInventoryHotbarSwap(player, itemStack, event) }
    }
    
    internal fun handleBlockBreakAction(player: Player, itemStack: ItemStack, event: BlockBreakActionEvent) {
        behaviors.forEach { it.handleBlockBreakAction(player, itemStack, event) }
    }
    
    internal fun handleRelease(player: Player, itemStack: ItemStack, event: ServerboundPlayerActionPacketEvent) {
        behaviors.forEach { it.handleRelease(player, itemStack, event) }
    }
    //</editor-fold>
    
    override fun toString() = id.toString()
    
    companion object {
        
        val CODEC = NovaRegistries.ITEM.byNameCodec()
        
    }
    
}