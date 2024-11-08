@file:Suppress("unused", "MemberVisibilityCanBePrivate", "UNCHECKED_CAST")

package xyz.xenondevs.nova.world.item

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.component.CustomData
import org.bukkit.Material
import org.bukkit.block.Block
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
import org.spongepowered.configurate.CommentedConfigurationNode
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.map
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.nova.config.Configs
import xyz.xenondevs.nova.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.builder.task.model.VanillaMaterialTypes
import xyz.xenondevs.nova.resources.layout.item.RequestedItemModelLayout
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.resources.model.ItemModelData
import xyz.xenondevs.nova.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.event.BlockBreakActionEvent
import xyz.xenondevs.nova.world.item.behavior.DefaultBehavior
import xyz.xenondevs.nova.world.item.behavior.ItemBehavior
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorFactory
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorHolder
import xyz.xenondevs.nova.world.item.logic.PacketItems
import xyz.xenondevs.nova.world.player.WrappedPlayerInteractEvent
import xyz.xenondevs.nova.world.player.equipment.ArmorEquipEvent
import kotlin.Unit
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf
import net.minecraft.world.item.ItemStack as MojangStack

/**
 * Represents an item type in Nova.
 */
class NovaItem internal constructor(
    val id: ResourceLocation,
    val name: Component?,
    val style: Style,
    behaviorHolders: List<ItemBehaviorHolder>,
    val maxStackSize: Int,
    private val _craftingRemainingItem: ItemStack?,
    val isHidden: Boolean,
    val block: NovaBlock?,
    configId: String,
    internal val requestedLayout: RequestedItemModelLayout
) {
    
    /**
     * The configuration for this [NovaItem].
     * May be an empty node if the config file does not exist.
     */
    val config: Provider<CommentedConfigurationNode> = Configs[configId]
    
    /**
     * The [ItemStack] that is left over after this [NovaItem] was
     * used in a crafting recipe.
     */
    val craftingRemainingItem: ItemStack?
        get() = _craftingRemainingItem?.clone()
    
    /**
     * The model data (vanilla item types and custom model data values) of this [NovaItem].
     */
    val model: ItemModelData by ResourceLookups.ITEM_MODEL_LOOKUP.provider.map {
        val material = vanillaMaterial
        val models = it[this]?.get(material) ?: emptyMap()
        ItemModelData(this, models)
    }
    
    /**
     * The [ItemBehaviors][ItemBehavior] of this [NovaItem].
     */
    val behaviors: List<ItemBehavior> = buildList {
        val behaviors = behaviorHolders.map { holder ->
            when (holder) {
                is ItemBehavior -> holder
                is ItemBehaviorFactory<*> -> holder.create(this@NovaItem)
            }
        }
        add(DefaultBehavior.create(this@NovaItem, behaviors))
        addAll(behaviors)
    }
    
    /**
     * The underlying vanilla material of this [NovaItem].
     */
    internal val vanillaMaterial: Material by combinedProvider(
        ResourceLookups.ITEM_MODEL_LOOKUP.provider,
        combinedProvider(behaviors.map(ItemBehavior::vanillaMaterialProperties))
    ) { lookup, properties ->
        var vanillaMaterial = VanillaMaterialTypes.getMaterial(properties.flatten().toHashSet())
        
        // fall back to first available material if vanilla material is not present in lookups
        val itemModels = lookup[this]
        if (itemModels != null && vanillaMaterial !in itemModels)
            vanillaMaterial = itemModels.keys.first()
        
        vanillaMaterial
    }
    
    /**
     * The base data components of this [NovaItem].
     */
    internal val baseDataComponents: DataComponentMap by combinedProvider(
        behaviors.map(ItemBehavior::baseDataComponents),
        ItemUtils::mergeDataComponentMaps
    )
    
    /**
     * The default components patch applied to all [ItemStacks][ItemStack] of this [NovaItem].
     */
    internal val defaultPatch: DataComponentPatch by combinedProvider(
        behaviors.map(ItemBehavior::defaultPatch),
        ItemUtils::mergeDataComponentPatches
    )
    
    /**
     * Creates an [ItemBuilder] for an [ItemStack] of this [NovaItem], in server-side format.
     */
    fun createItemBuilder(modelId: String = "default"): ItemBuilder =
        ItemBuilder(createItemStack(1, modelId))
    
    /**
     * Creates an [ItemStack] of this [NovaItem] with the given [amount] and [modelId] in server-side format.
     */
    fun createItemStack(amount: Int = 1, modelId: String = "default"): ItemStack =
        createItemStack { putString("modelId", modelId) }.also { it.amount = amount }
    
    private fun createItemStack(writeModelId: CompoundTag.() -> Unit): ItemStack {
        val itemStack = MojangStack(PacketItems.SERVER_SIDE_ITEM_HOLDER, 1, defaultPatch)
        itemStack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY) { customData ->
            customData.update { compoundTag ->
                val novaCompound = compoundTag.getCompound("nova") // should be present in defaultPatch
                writeModelId(novaCompound)
            }
        }
        
        return itemStack.asBukkitMirror()
    }
    
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
    
    /**
     * Modifies the block [damage] of this [NovaItem] when using [itemStack] to break [block].
     */
    internal fun modifyBlockDamage(player: Player, itemStack: ItemStack, block: Block, damage: Double): Double {
        return behaviors.fold(damage) { currentDamage, behavior -> behavior.modifyBlockDamage(player, itemStack, block, currentDamage) }
    }
    
    /**
     * Modifies the client-side stack of this [NovaItem], in the context that it is sent to [player] and has [data].
     */
    internal fun modifyClientSideStack(player: Player?, itemStack: ItemStack, data: NamespacedCompound): ItemStack {
        return behaviors.fold(itemStack) { stack, behavior -> behavior.modifyClientSideStack(player, stack, data) }
    }
    
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