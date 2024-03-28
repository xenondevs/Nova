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
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.ItemStack
import org.spongepowered.configurate.kotlin.extensions.get
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.nmsutils.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.ConfigProvider
import xyz.xenondevs.nova.data.config.Configs
import xyz.xenondevs.nova.data.resources.builder.task.model.VanillaMaterialTypes
import xyz.xenondevs.nova.data.resources.layout.item.RequestedItemModelLayout
import xyz.xenondevs.nova.data.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.data.resources.model.ItemModelData
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.item.behavior.ItemBehaviorFactory
import xyz.xenondevs.nova.item.behavior.ItemBehaviorHolder
import xyz.xenondevs.nova.item.logic.PacketItemData
import xyz.xenondevs.nova.item.logic.PacketItems
import xyz.xenondevs.nova.item.vanilla.AttributeModifier
import xyz.xenondevs.nova.player.WrappedPlayerInteractEvent
import xyz.xenondevs.nova.player.equipment.ArmorEquipEvent
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.data.CentralizedLazy
import xyz.xenondevs.nova.util.data.logExceptionMessages
import xyz.xenondevs.nova.util.item.novaCompound
import xyz.xenondevs.nova.util.item.novaCompoundOrNull
import xyz.xenondevs.nova.util.item.unhandledTags
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
    private val _maxStackSize: Int,
    val craftingRemainingItem: ItemBuilder?,
    val isHidden: Boolean,
    val block: NovaBlock?,
    private val configId: String,
    internal val requestedLayout: RequestedItemModelLayout
) {
    
    /**
     * The maximum stack size of this [NovaItem].
     */
    val maxStackSize: Int
        get() = min(_maxStackSize, vanillaMaterial.maxStackSize)
    
    val model: ItemModelData by lazy {
        val material = vanillaMaterial
        val namedModels = ResourceLookups.NAMED_ITEM_MODEL[this]?.get(material)
            ?: throw IllegalStateException("Could not retrieve named models for $this, $material")
        val unnamedModels = ResourceLookups.UNNAMED_ITEM_MODEL[this]?.get(material)
            ?: throw IllegalStateException("Could not retrieve unnamed models for $this, $material")
        return@lazy ItemModelData(this, material, namedModels, unnamedModels)
    }
    
    /**
     * The configuration for this [NovaItem].
     * Trying to read config values from this when no config is present will result in an exception.
     *
     * Use the extension functions `entry` and `optionalEntry` to get values from the config.
     */
    val config: ConfigProvider = Configs[configId]
    
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
    fun createItemBuilder(modelId: String = "default"): ItemBuilder =
        createItemBuilder { putString("modelId", modelId) }
    
    /**
     * Creates an [ItemBuilder] for an [ItemStack] of this [NovaItem], in server-side format.
     */
    fun createItemBuilder(modelId: Int): ItemBuilder =
        createItemBuilder { putInt("subId", modelId) }
    
    @Suppress("DEPRECATION")
    private fun createItemBuilder(writeModelId: CompoundTag.() -> Unit): ItemBuilder =
        ItemBuilder(PacketItems.SERVER_SIDE_MATERIAL)
            .let { initialBuilder -> behaviors.fold(initialBuilder) { acc, behavior -> behavior.modifyItemBuilder(acc) } }
            .addModifier { itemStack ->
                val novaCompoundTag = CompoundTag()
                novaCompoundTag.putString("id", id.toString())
                writeModelId(novaCompoundTag)
                val meta = itemStack.itemMeta!!
                meta.unhandledTags["nova"] = novaCompoundTag
                itemStack.itemMeta = meta
                
                if (defaultCompound != null)
                    itemStack.novaCompound.putAll(defaultCompound!!.copy())
                
                itemStack
            }
    
    /**
     * Creates an [ItemStack] of this [NovaItem] with the given [amount] and [modelId] in server-side format.
     */
    fun createItemStack(amount: Int = 1, modelId: String = "default"): ItemStack =
        createItemBuilder(modelId).setAmount(amount).get()
    
    /**
     * Creates an [ItemStack] of this [NovaItem] with the given [amount] and [modelId] in server-side format.
     */
    fun createItemStack(amount: Int = 1, modelId: Int): ItemStack =
        createItemBuilder(modelId).setAmount(amount).get()
    
    /**
     * Checks whether this [NovaItem] has an [ItemBehavior] of the reified type [T], or a subclass of it.
     */
    inline fun <reified T : Any> hasBehavior(): Boolean =
        hasBehavior(T::class)
    
    /**
     * Checks whether this [NovaItem] has an [ItemBehavior] of the specified class [type], or a subclass of it.
     */
    fun <T : Any> hasBehavior(type: KClass<T>): Boolean =
        behaviors.any { type.isSuperclassOf(it::class) }
    
    /**
     * Gets the first [ItemBehavior] that is an instance of [T], or null if there is none.
     */
    inline fun <reified T : Any> getBehaviorOrNull(): T? =
        getBehaviorOrNull(T::class)
    
    /**
     * Gets the first [ItemBehavior] that is an instance of [type] or a subclass, or null if there is none.
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
    
    internal fun getPacketItemData(itemStack: MojangStack?): PacketItemData {
        val itemData = PacketItemData(itemStack?.orCreateTag ?: CompoundTag(), name)
        behaviors.forEach { it.updatePacketItemData(itemStack?.novaCompoundOrNull ?: NamespacedCompound.EMPTY, itemData) }
        return itemData
    }
    
    //<editor-fold desc="load methods", defaultstate="collapsed">
    private fun reload() {
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
    internal fun handleInteract(player: Player, itemStack: ItemStack, action: Action, wrappedEvent: WrappedPlayerInteractEvent) {
        behaviors.forEach { it.handleInteract(player, itemStack, action, wrappedEvent) }
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