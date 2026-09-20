package xyz.xenondevs.nova.world.item

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.phys.Vec3
import org.bukkit.block.Block
import org.bukkit.block.BlockType
import org.bukkit.craftbukkit.inventory.CraftItemType
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.flatten
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.ItemWrapper
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.config.ConfigProvider
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.context.intention.EntityInteract
import xyz.xenondevs.nova.context.intention.ItemUse
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.registry.NovaItemBuilder
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.bootstrapFlatMap
import xyz.xenondevs.nova.util.asBukkitCopy
import xyz.xenondevs.nova.util.blockFace
import xyz.xenondevs.nova.util.bukkitEquipmentSlot
import xyz.xenondevs.nova.util.concurrent.checkServerThread
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.nmsItem
import xyz.xenondevs.nova.util.toBlock
import xyz.xenondevs.nova.util.toIdentifier
import xyz.xenondevs.nova.util.toVector3d
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.block.event.BlockBreakActionEvent
import xyz.xenondevs.nova.world.item.behavior.ItemBehavior
import xyz.xenondevs.nova.world.item.logic.PacketItems
import xyz.xenondevs.nova.world.toNms
import java.lang.invoke.MethodHandles
import java.lang.invoke.VarHandle
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf
import net.minecraft.world.InteractionResult as NmsInteractionResult
import net.minecraft.world.entity.Entity as NmsEntity
import net.minecraft.world.entity.player.Player as NmsPlayer
import net.minecraft.world.item.ItemStack as NmsItemStack

private val ITEM_CACHED_TYPE: VarHandle = MethodHandles
    .privateLookupIn(Item::class.java, MethodHandles.lookup())
    .findVarHandle(Item::class.java, $$"nova$cachedType", ItemType::class.java)

private val ITEM_CACHED_TYPE_ENTRY: VarHandle = MethodHandles
    .privateLookupIn(Item::class.java, MethodHandles.lookup())
    .findVarHandle(Item::class.java, $$"nova$cachedTypeEntry", Any::class.java)

private val ITEM_CACHED_ITEM_PROVIDER: VarHandle = MethodHandles
    .privateLookupIn(Item::class.java, MethodHandles.lookup())
    .findVarHandle(Item::class.java, $$"nova$itemProvider", Any::class.java)

private val ITEM_CACHED_GUI_ITEM_PROVIDER: VarHandle = MethodHandles
    .privateLookupIn(Item::class.java, MethodHandles.lookup())
    .findVarHandle(Item::class.java, $$"nova$guiItemProvider", Any::class.java)

private val Item.itemType: ItemType
    get() {
        val cached = ITEM_CACHED_TYPE.get(this)
        if (cached != null)
            return cached as ItemType
        val itemType = CraftItemType.minecraftToBukkitNew(this)
        ITEM_CACHED_TYPE.set(this, itemType)
        return itemType
    }

@Suppress("UNCHECKED_CAST")
private val Item.itemTypeEntry: RegistryEntry.Paper<ItemType>
    get() {
        val cached = ITEM_CACHED_TYPE_ENTRY.get(this)
        if (cached != null)
            return cached as RegistryEntry.Paper<ItemType>
        val entry = RegistryEntry.paper(RegistryKey.ITEM, itemType)
        ITEM_CACHED_TYPE_ENTRY.set(this, entry)
        return entry
    }

@Suppress("UNCHECKED_CAST")
private val Item.itemProvider: Provider<ItemProvider>
    get() {
        val cached = ITEM_CACHED_TYPE_ENTRY.get(this)
        if (cached != null)
            return cached as Provider<ItemProvider>
        val provider = provider(ItemWrapper(itemType.createItemStack()))
        ITEM_CACHED_ITEM_PROVIDER.set(this, provider)
        return provider
    }

@Suppress("UNCHECKED_CAST")
private val Item.guiItemProvider: Provider<ItemProvider>
    get() {
        val cached = ITEM_CACHED_GUI_ITEM_PROVIDER.get(this)
        if (cached != null)
            return cached as Provider<ItemProvider>
        val provider = provider(ItemWrapper(itemType.createItemStack().apply {
            editPersistentDataContainer { pdc ->
                pdc.set(PacketItems.ADVANCED_TOOLTIP_OVERRIDE, PersistentDataType.BOOLEAN, false)
            }
        }))
        ITEM_CACHED_GUI_ITEM_PROVIDER.set(this, provider)
        return provider
    }

internal val ItemStack.novaItem: NovaItem?
    get() = unwrap().item as? NovaItem

internal val NmsItemStack.novaItem: NovaItem?
    get() = item as? NovaItem

@PublishedApi
internal val ItemType.novaItem: NovaItem?
    get() = (this as CraftItemType<*>).handle as? NovaItem

@PublishedApi
internal fun <T : Any> ItemType.hasBehavior(type: KClass<T>): Boolean =
    novaItem?.behaviors?.any { type.isSuperclassOf(it::class) } == true

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal fun <T : Any> ItemType.getBehaviorOrNull(type: KClass<T>): T? =
    novaItem?.behaviors?.firstOrNull { type.isSuperclassOf(it::class) } as T?

@PublishedApi
internal fun <T : Any> ItemType.getBehaviorOrThrow(type: KClass<T>): T =
    getBehaviorOrNull(type) ?: throw NoSuchElementException("${key.asString()} has no behavior of type ${type.simpleName}")

val ItemStack.itemType: ItemType
    get() = unwrap().item.itemType

val ItemStack.itemTypeEntry: RegistryEntry.Paper<ItemType>
    get() = unwrap().item.itemTypeEntry

val ItemType.blockTypeOrNull: BlockType?
    get() = if (hasBlockType()) blockType else null

/**
 * Gets whether this [ItemType] is a custom item from Nova.
 */
val ItemType.isNova: Boolean
    get() = novaItem != null

val ItemType.name: Component
    get() = getDefaultData(DataComponentTypes.ITEM_NAME) ?: Component.empty()

/**
 * Gets whether this [ItemType][ItemType] [is hidden][NovaItemBuilder.hidden]. `false` for non-Nova items.
 */
val ItemType.isHidden: Boolean
    get() = novaItem?.isHidden ?: false

fun ItemType.hasBehavior(behavior: ItemBehavior): Boolean =
    novaItem?.behaviors?.contains(behavior) == true

inline fun <reified T : Any> ItemType.hasBehavior(): Boolean =
    hasBehavior(T::class)

inline fun <reified T : Any> ItemType.getBehaviorOrNull(): T? =
    getBehaviorOrNull(T::class)

inline fun <reified T : Any> ItemType.getBehaviorOrThrow(): T =
    getBehaviorOrThrow(T::class)

/**
 * Creates an [ItemStack] for the [ItemType] of the [RegistryEntry].
 * 
 * Cannot be called during bootstrap (pre-registry-freeze).
 */
fun Provider<ItemType>.createItemStack(amount: Int = 1): ItemStack =
    get().createItemStack(amount)

/**
 * Shortcut for `bootstrapFlatMap { it.config }` 
 */
val Provider<ItemType>.config: Provider<ConfigProvider>
    get() = bootstrapFlatMap { it.config }

/**
 * Gets the type's config if [ItemType.isNova], otherwise [ConfigProvider.Empty].
 */
val ItemType.config: Provider<ConfigProvider>
    get() = novaItem?.config ?: provider(ConfigProvider.Empty)

/**
 * Shortcut for `bootstrapFlatMap { it.itemProvider }`
 */
val RegistryEntry.Paper<ItemType>.itemProvider: Provider<ItemProvider>
    get() = bootstrapFlatMap { it.itemProvider }

/**
 * Shortcut for `bootstrapFlatMap { it.guiItemProvider }`
 */
val RegistryEntry.Paper<ItemType>.guiItemProvider: Provider<ItemProvider>
    get() = bootstrapFlatMap { it.guiItemProvider }

/**
 * An [ItemProvider] provider of an [ItemStack] with size 1 of this [ItemType], intended for usage in InvUI DSLs.
 */
val ItemType.itemProvider: Provider<ItemProvider>
    get() = nmsItem.itemProvider

/**
 * An [ItemProvider] provider of an [ItemStack] with size 1 of this [ItemType], intended for usage in InvUI DSLs.
 * 
 * The only difference to [itemProvider] is that this one does not have advanced tooltips (`/nova advancedTooltips`).
 */
val ItemType.guiItemProvider: Provider<ItemProvider>
    get() = nmsItem.guiItemProvider

@Deprecated("", ReplaceWith("guiItemProvider"))
val RegistryEntry.Paper<ItemType>.clientsideProvider: Provider<ItemProvider>
    get() = guiItemProvider

@Deprecated("", ReplaceWith("guiItemProvider"))
val ItemType.clientsideProvider: Provider<ItemProvider>
    get() = guiItemProvider

/**
 * Represents a custom Nova item type.
 */
internal class NovaItem(
    val entry: RegistryEntry.Paper<ItemType>,
    behaviors: Provider<List<ItemBehavior>>,
    craftingRemainingItem: Provider<RegistryEntry.Paper<ItemType>>,
    isHidden: Provider<Boolean>,
    block: Provider<RegistryEntry.Paper<BlockType>?>,
    val config: Provider<ConfigProvider>,
) : Item(Properties().setId(ResourceKey.create(Registries.ITEM, entry.key.toIdentifier()))) {
    
    val key: Key
        get() = entry.key
    
    val behaviors: List<ItemBehavior> by behaviors
    val isHidden: Boolean by isHidden
    val block: BlockType? by block.flatten()
    
    private val craftRemainder: ItemStackTemplate?
        by craftingRemainingItem.flatten().map { ItemStackTemplate(it.nmsItem) }
    
    override fun getCraftingRemainder() = craftRemainder
    
    /**
     * The base data components of this [NovaItem].
     */
    val baseDataComponents: DataComponentMap by behaviors.flatMap { behaviors ->
        combinedProvider(behaviors.map(ItemBehavior::baseDataComponents)) { maps ->
            DataComponentMap(ItemUtils.mergeDataComponentMaps(maps.map(DataComponentMap::handle)))
        }
    }
    
    /**
     * Checks whether this [NovaItem] has an [ItemBehavior] of the reified type [T], or a subclass of it.
     */
    fun <T : Any> hasBehavior(type: Class<T>): Boolean =
        this@NovaItem.behaviors.any { type.isAssignableFrom(it::class.java) }
    
    /**
     * Gets the first [ItemBehavior] that is an instance of [T], or null if there is none.
     */
    fun <T : Any> getBehaviorOrNull(type: Class<T>): T? =
        this@NovaItem.behaviors.firstOrNull { type.isAssignableFrom(it::class.java) } as T?
    
    /**
     * Gets the first [ItemBehavior] that is an instance of [T], or throws an [IllegalStateException] if there is none.
     */
    fun <T : Any> getBehaviorOrThrow(behavior: Class<T>): T =
        getBehaviorOrNull(behavior) ?: throw IllegalStateException("Item ${key.asString()} does not have a behavior of type ${behavior.simpleName}")
    
    //<editor-fold desc="item behavior functionality", defaultstate="collapsed">
    /**
     * Modifies the block [damage] of this [NovaItem] when using [itemStack] to break [block].
     */
    fun modifyBlockDamage(
        player: Player,
        itemStack: ItemStack,
        block: Block,
        damage: Double
    ): Double = runSafely("modify block damage", damage) {
        this@NovaItem.behaviors.fold(damage) { currentDamage, behavior ->
            behavior.modifyBlockDamage(player, itemStack.clone(), block, currentDamage)
        }
    }
    
    /**
     * Modifies the client-side item type of this [NovaItem], in the context that it is sent to [player] and has [server] data.
     */
    fun modifyClientSideItemType(
        player: Player?,
        server: ItemStack,
        client: ItemType
    ): ItemType = runSafely("modify client-side item type", client, allowOffMain = true) {
        this@NovaItem.behaviors.fold(client) { current, behavior -> behavior.modifyClientSideItemType(player, server.clone(), current) }
    }
    
    /**
     * Modifies the client-side stack of this [NovaItem], in the context that it is sent to [player] and has [data].
     */
    fun modifyClientSideStack(
        player: Player?,
        server: ItemStack,
        client: ItemStack
    ): ItemStack = runSafely("modify client-side stack", client, allowOffMain = true) {
        this@NovaItem.behaviors.fold(client.clone()) { stack, behavior -> behavior.modifyClientSideStack(player, server.clone(), stack) }
    }
    
    internal fun useNms(
        nmsItemStack: NmsItemStack,
        nmsPlayer: NmsPlayer,
        nmsHand: InteractionHand
    ): NmsInteractionResult {
        // check cooldown since Nova applies cooldowns in all item-use cases
        if (nmsPlayer.cooldowns.isOnCooldown(nmsItemStack))
            return NmsInteractionResult.PASS
        
        val player = nmsPlayer.bukkitEntity
        val itemStack = nmsItemStack.asBukkitCopy()
        val hand = nmsHand.bukkitEquipmentSlot
        
        if (player is Player && !ProtectionManager.canUseItem(player, itemStack, player.location))
            return NmsInteractionResult.FAIL
        
        val ctx = Context.intention(ItemUse)
            .param(ItemUse.HELD_ITEM_STACK, itemStack)
            .param(ItemUse.SOURCE_ENTITY, player)
            .param(ItemUse.HELD_HAND, hand)
            .build()
        
        val result = use(ctx)
        if (result is InteractionResult.Success)
            result.performActions(player, hand)
        return result.toNms()
    }
    
    /**
     * Handles using this [NovaItem] in the given [ctx].
     */
    fun use(
        ctx: Context<ItemUse>
    ): InteractionResult = runSafely("handle use", InteractionResult.Fail) {
        for (behavior in this@NovaItem.behaviors) {
            val result = behavior.use(ctx[ItemUse.HELD_ITEM_STACK], ctx)
            if (result !is InteractionResult.Pass)
                return result
        }
        return InteractionResult.Pass
    }
    
    internal fun useOnBlockNms(
        nmsItemStack: NmsItemStack,
        nmsCtx: UseOnContext
    ): NmsInteractionResult {
        // check cooldown since Nova applies cooldowns in all item-use cases
        if (nmsCtx.player?.cooldowns?.isOnCooldown(nmsItemStack) == true)
            return NmsInteractionResult.PASS
        
        val player = nmsCtx.player?.bukkitEntity
        val itemStack = nmsItemStack.asBukkitCopy()
        val hand = nmsCtx.hand.bukkitEquipmentSlot
        val pos = nmsCtx.clickedPos.toBlock(nmsCtx.level.world)
        val face = nmsCtx.clickedFace.blockFace
        
        if (player is Player && !ProtectionManager.canUseBlock(player, itemStack, pos))
            return NmsInteractionResult.FAIL
        
        val ctx = Context.intention(BlockInteract)
            .param(BlockInteract.BLOCK, pos)
            .param(BlockInteract.SOURCE_ENTITY, player)
            .param(BlockInteract.HELD_ITEM_STACK, itemStack)
            .param(BlockInteract.HELD_HAND, hand)
            .param(BlockInteract.CLICKED_BLOCK_FACE, face)
            .build()
        
        val result = useOnBlock(ctx)
        if (player != null && result is InteractionResult.Success)
            result.performActions(player, hand)
        return result.toNms()
    }
    
    /**
     * Handles using this [NovaItem] on a block in the given [ctx].
     */
    fun useOnBlock(
        ctx: Context<BlockInteract>
    ): InteractionResult = runSafely("handle use on", InteractionResult.Fail) {
        for (behavior in this@NovaItem.behaviors) {
            val result = behavior.useOnBlock(ctx[BlockInteract.HELD_ITEM_STACK], ctx[BlockInteract.BLOCK], ctx)
            if (result !is InteractionResult.Pass)
                return result
        }
        return InteractionResult.Pass
    }
    
    internal fun useOnEntityNms(
        nmsPlayer: NmsPlayer,
        nmsItemStack: NmsItemStack,
        nmsTarget: NmsEntity,
        nmsHand: InteractionHand,
        nmsInteractLoc: Vec3
    ): NmsInteractionResult {
        // check cooldown since Nova applies cooldowns in all item-use cases
        if (nmsPlayer.cooldowns.isOnCooldown(nmsItemStack))
            return NmsInteractionResult.PASS
        
        val player = nmsPlayer.bukkitEntity
        val itemStack = nmsItemStack.asBukkitCopy()
        val target = nmsTarget.bukkitEntity
        val hand = nmsHand.bukkitEquipmentSlot
        val interactLoc = nmsInteractLoc.toVector3d()
        
        if (player is Player && !ProtectionManager.canInteractWithEntity(player, target, itemStack))
            return NmsInteractionResult.FAIL
        
        val ctx = Context.intention(EntityInteract)
            .param(EntityInteract.HELD_ITEM_STACK, itemStack)
            .param(EntityInteract.SOURCE_ENTITY, player)
            .param(EntityInteract.TARGET_ENTITY, target)
            .param(EntityInteract.HELD_HAND, hand)
            .param(EntityInteract.INTERACT_LOCATION, interactLoc)
            .build()
        
        val result = useOnEntity(ctx)
        if (result is InteractionResult.Success)
            result.performActions(player, hand)
        return result.toNms()
    }
    
    /**
     * Handles using this [NovaItem] on an entity in the given [ctx].
     */
    fun useOnEntity(
        ctx: Context<EntityInteract>
    ): InteractionResult = runSafely("handle use on living entity", InteractionResult.Fail) {
        for (behavior in this@NovaItem.behaviors) {
            val result = behavior.useOnEntity(ctx[EntityInteract.HELD_ITEM_STACK], ctx[EntityInteract.TARGET_ENTITY], ctx)
            if (result !is InteractionResult.Pass)
                return result
        }
        return InteractionResult.Pass
    }
    
    /**
     * Handles [event] where [player] attacks [attacked] using [itemStack] with this [NovaItem] in their main hand.
     */
    fun handleAttackEntity(
        player: Player,
        itemStack: ItemStack,
        attacked: Entity,
        event: EntityDamageByEntityEvent
    ): Unit = runSafely("handle attack entity") {
        this@NovaItem.behaviors.forEach { it.handleAttackEntity(player, itemStack.clone(), attacked, event) }
    }
    
    /**
     * Handles [event] where [player] breaks a block using [itemStack] with this [NovaItem].
     */
    fun handleBreakBlock(
        player: Player,
        itemStack: ItemStack,
        event: BlockBreakEvent
    ): Unit = runSafely("handle break block") {
        this@NovaItem.behaviors.forEach { it.handleBreakBlock(player, itemStack.clone(), event) }
    }
    
    /**
     * Handles [event] where [player] damages [itemStack] with this [NovaItem].
     */
    fun handleDamage(
        player: Player,
        itemStack: ItemStack,
        event: PlayerItemDamageEvent
    ): Unit = runSafely("handle damage") {
        this@NovaItem.behaviors.forEach { it.handleDamage(player, itemStack.clone(), event) }
    }
    
    /**
     * Handles [event] where [player] breaks [itemStack] with this [NovaItem].
     */
    fun handleBreak(
        player: Player,
        itemStack: ItemStack,
        event: PlayerItemBreakEvent
    ): Unit = runSafely("handle break") {
        this@NovaItem.behaviors.forEach { it.handleBreak(player, itemStack.clone(), event) }
    }
    
    /**
     * Handles [event] where [player] equips or unequips ([equipped]) [itemStack] with this [NovaItem] in [slot].
     */
    fun handleEquip(
        player: Player,
        itemStack: ItemStack,
        slot: EquipmentSlot,
        equipped: Boolean,
        event: EntityEquipmentChangedEvent
    ): Unit = runSafely("handle equip") {
        this@NovaItem.behaviors.forEach { it.handleEquip(player, itemStack.clone(), slot, equipped, event) }
    }
    
    /**
     * Handles [event] where [player] clicks on [itemStack] with this [NovaItem] in an inventory.
     */
    fun handleInventoryClick(
        player: Player,
        itemStack: ItemStack,
        event: InventoryClickEvent
    ): Unit = runSafely("handle inventory click") {
        this@NovaItem.behaviors.forEach { it.handleInventoryClick(player, itemStack.clone(), event) }
    }
    
    /**
     * Handles [event] where [player] clicks on a slot with [itemStack] with this [NovaItem] on their cursor.
     */
    fun handleInventoryClickOnCursor(
        player: Player,
        itemStack: ItemStack,
        event: InventoryClickEvent
    ): Unit = runSafely("handle inventory click on cursor") {
        this@NovaItem.behaviors.forEach { it.handleInventoryClickOnCursor(player, itemStack.clone(), event) }
    }
    
    /**
     * Handles [event] where [player] swaps [itemStack] with this [NovaItem] via hotbar swap.
     */
    fun handleInventoryHotbarSwap(
        player: Player,
        itemStack: ItemStack,
        event: InventoryClickEvent
    ): Unit = runSafely("handle inventory hotbar swap") {
        this@NovaItem.behaviors.forEach { it.handleInventoryHotbarSwap(player, itemStack.clone(), event) }
    }
    
    /**
     * Handles [event] where [player] performs a block break action using [itemStack] with this [NovaItem].
     */
    fun handleBlockBreakAction(
        player: Player,
        itemStack: ItemStack,
        event: BlockBreakActionEvent
    ): Unit = runSafely("handle block break action") {
        this@NovaItem.behaviors.forEach { it.handleBlockBreakAction(player, itemStack.clone(), event) }
    }
    
    /**
     * Handles [event] where [player] consumes [itemStack] with this [NovaItem].
     */
    fun handleConsume(
        player: Player,
        itemStack: ItemStack,
        event: PlayerItemConsumeEvent
    ): Unit = runSafely("handle consume") {
        this@NovaItem.behaviors.forEach { it.handleConsume(player, itemStack.clone(), event) }
    }
    
    /**
     * Handles an inventory tick for [player] with [itemStack] with this [NovaItem] in [slot].
     */
    fun handleInventoryTick(
        player: Player,
        itemStack: ItemStack,
        slot: Int
    ): Unit = runSafely("handle inventory tick") {
        this@NovaItem.behaviors.forEach { it.handleInventoryTick(player, itemStack.clone(), slot) }
    }
    
    /**
     * Handles an equipment tick for [player] with [itemStack] with this [NovaItem] in [slot].
     */
    fun handleEquipmentTick(
        player: Player,
        itemStack: ItemStack,
        slot: EquipmentSlot
    ): Unit = runSafely("handle equipment tick") {
        this@NovaItem.behaviors.forEach { it.handleEquipmentTick(player, itemStack.clone(), slot) }
    }
    
    /**
     * Handles a use tick for [entity] with [itemStack] with this [NovaItem] in [hand]
     * with [passedUseTicks] passed and [remainingUseTicks] remaining.
     */
    fun handleUseTick(
        entity: LivingEntity,
        itemStack: ItemStack,
        hand: EquipmentSlot,
        passedUseTicks: Int,
        remainingUseTicks: Int
    ): Unit = runSafely("handle use tick") {
        this@NovaItem.behaviors.forEach { it.handleUseTick(entity, itemStack.clone(), hand, remainingUseTicks) }
    }
    
    /**
     * Handles the use of [itemStack] with this [NovaItem] finishing for [entity] in [hand].
     */
    fun handleUseFinished(
        entity: LivingEntity,
        itemStack: ItemStack,
        hand: EquipmentSlot,
    ): ItemAction = runSafely("handle use finished", ItemAction.None) {
        ItemAction.Composite(this@NovaItem.behaviors.map { it.handleUseFinished(entity, itemStack.clone(), hand) })
    }
    
    /**
     * Handles the use of [itemStack] with this [NovaItem] being stopped for [entity] in [hand]
     * and [remainingUseTicks] left.
     */
    fun handleUseStopped(
        entity: LivingEntity,
        itemStack: ItemStack,
        hand: EquipmentSlot,
        remainingUseTicks: Int
    ): Unit = runSafely("handle use stopped") {
        this@NovaItem.behaviors.forEach { it.handleUseStopped(entity, itemStack.clone(), hand, remainingUseTicks) }
    }
    
    /**
     * Modifies the use [duration] of [itemStack] with this [NovaItem] for [entity].
     */
    fun modifyUseDuration(
        entity: LivingEntity,
        itemStack: ItemStack,
        duration: Int
    ): Int = runSafely("modify use duration", duration) {
        this@NovaItem.behaviors.fold(duration) { currentDuration, behavior ->
            behavior.modifyUseDuration(entity, itemStack.clone(), currentDuration)
        }
    }
    
    internal inline fun runSafely(
        name: String,
        allowOffMain: Boolean = false,
        run: () -> Unit
    ) = runSafely(name, Unit, allowOffMain, run)
    
    internal inline fun <T> runSafely(
        name: String,
        fallback: T,
        allowOffMain: Boolean = false,
        run: () -> T
    ): T {
        if (!allowOffMain)
            checkServerThread()
        try {
            return run()
        } catch (t: Throwable) {
            LOGGER.error("Failed to $name for ${key.asString()}", t)
        }
        return fallback
    }
    
    //</editor-fold>
    
    override fun toString(): String = key.asString()
    
}
