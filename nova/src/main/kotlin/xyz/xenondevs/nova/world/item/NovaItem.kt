@file:Suppress("unused", "MemberVisibilityCanBePrivate", "UNCHECKED_CAST")

package xyz.xenondevs.nova.world.item

import io.papermc.paper.event.entity.EntityEquipmentChangedEvent
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.phys.Vec3
import org.bukkit.Material
import org.bukkit.block.Block
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
import org.spongepowered.configurate.CommentedConfigurationNode
import xyz.xenondevs.cbf.Cbf
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.ItemWrapper
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.config.Configs
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.context.intention.EntityInteract
import xyz.xenondevs.nova.context.intention.ItemUse
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.builder.layout.item.ItemModelDefinitionBuilder
import xyz.xenondevs.nova.resources.builder.layout.item.ItemModelSelectorScope
import xyz.xenondevs.nova.resources.builder.task.VanillaMaterialTypes
import xyz.xenondevs.nova.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.util.blockFace
import xyz.xenondevs.nova.util.bukkitEquipmentSlot
import xyz.xenondevs.nova.util.concurrent.checkServerThread
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.toNovaPos
import xyz.xenondevs.nova.util.toVector3d
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.event.BlockBreakActionEvent
import xyz.xenondevs.nova.world.item.behavior.BlockItemBehavior
import xyz.xenondevs.nova.world.item.behavior.DefaultBehavior
import xyz.xenondevs.nova.world.item.behavior.ItemBehavior
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorFactory
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorHolder
import xyz.xenondevs.nova.world.item.logic.PacketItems
import xyz.xenondevs.nova.world.toNms
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf
import net.minecraft.world.InteractionResult as NmsInteractionResult
import net.minecraft.world.entity.Entity as NmsEntity
import net.minecraft.world.entity.player.Player as NmsPlayer
import net.minecraft.world.item.ItemStack as NmsItemStack

/**
 * Represents a custom Nova item type.
 */
class NovaItem internal constructor(
    val id: Key,
    val name: Component?,
    val lore: List<Component>,
    val style: Style,
    behaviorHolders: List<ItemBehaviorHolder>,
    val maxStackSize: Int,
    private val _craftingRemainingItem: Key?,
    val isHidden: Boolean,
    val block: NovaBlock?,
    configId: String,
    val tooltipStyle: TooltipStyle?,
    internal val configureDefinition: ItemModelDefinitionBuilder<ItemModelSelectorScope>.() -> Unit
) {
    
    /**
     * The configuration for this [NovaItem].
     * May be an empty node if the config file does not exist.
     */
    val config: Provider<CommentedConfigurationNode> = Configs[configId]
    
    /**
     * The [ItemStack] that is left over after this [NovaItem] was
     * used in a crafting recipe, or an empty stack if there is no remainder.
     */
    val craftingRemainingItem: ItemStack
        get() = _craftingRemainingItem?.let(ItemUtils::getItemStack) ?: ItemStack.empty()
    
    /**
     * The [ItemBehaviors][ItemBehavior] of this [NovaItem].
     */
    val behaviors: List<ItemBehavior> = buildList {
        add(DefaultBehavior.create(this@NovaItem))
        if (block != null)
            add(BlockItemBehavior(provider(block)))
        for (holder in behaviorHolders) {
            when (holder) {
                is ItemBehavior -> add(holder)
                is ItemBehaviorFactory<*> -> add(holder.create(this@NovaItem))
            }
        }
    }
    
    /**
     * An [ItemProvider] containing the client-side [ItemStack] of this [NovaItem],
     * intended for use in [Guis][Gui].
     */
    val clientsideProvider: ItemProvider by lazy {
        val clientStack = PacketItems.getClientSideStack(
            player = null,
            itemStack = createItemStack().unwrap(),
            storeServerSideTag = false
        )
        
        // remove existing custom data and tag item to not receive server-side tooltip (again)
        clientStack.set(DataComponents.CUSTOM_DATA, CustomData.of(CompoundTag().apply {
            putBoolean(PacketItems.SKIP_SERVER_SIDE_TOOLTIP, true)
        }))
        
        ItemWrapper(clientStack.asBukkitMirror())
    }
    
    /**
     * The underlying vanilla material of this [NovaItem].
     */
    internal val vanillaMaterial: Material by combinedProvider(
        behaviors.map(ItemBehavior::vanillaMaterialProperties)
    ) { properties -> VanillaMaterialTypes.getMaterial(properties.flatten().toHashSet()) }
    
    /**
     * The base data components of this [NovaItem].
     */
    val baseDataComponents: DataComponentMap by combinedProvider(
        behaviors.map(ItemBehavior::baseDataComponents)
    ) { maps -> DataComponentMap(ItemUtils.mergeDataComponentMaps(maps.map(DataComponentMap::handle))) }
    
    /**
     * The default components patch applied to all [ItemStacks][ItemStack] of this [NovaItem].
     */
    internal val defaultPatch: DataComponentPatch by combinedProvider(
        behaviors.map(ItemBehavior::defaultCompound)
    ) { defaultCompounds ->
        val defaultCompound = NamespacedCompound()
        for (defaultCompound in defaultCompounds) {
            defaultCompound.putAll(defaultCompound)
        }
        
        DataComponentPatch.builder()
            .set(DataComponents.CUSTOM_DATA, CustomData.of(CompoundTag().also { compoundTag ->
                compoundTag.put("nova", CompoundTag().also {
                    it.putString("id", id.toString())
                })
                if (defaultCompound.isNotEmpty()) {
                    compoundTag.putByteArray("nova_cbf", Cbf.write(defaultCompound))
                }
            }))
            .build()
    }
    
    /**
     * Creates an [ItemBuilder] for an [ItemStack] of this [NovaItem], in server-side format.
     */
    fun createItemBuilder(): ItemBuilder =
        ItemBuilder(createItemStack(1))
    
    /**
     * Creates an [ItemStack] of this [NovaItem] with the given [amount] in server-side format.
     */
    fun createItemStack(amount: Int = 1): ItemStack =
        NmsItemStack(PacketItems.SERVER_SIDE_ITEM_HOLDER, amount, defaultPatch).asBukkitMirror()
    
    /**
     * Creates an [ItemBuilder] for an [ItemStack] of this [NovaItem], in client-side format,
     * intended for use in [Guis][Gui].
     */
    fun createClientsideItemBuilder(): ItemBuilder =
        ItemBuilder(clientsideProvider.get())
    
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
     * Checks whether this [NovaItem] has an [ItemBehavior] of the specified class [type], or a subclass of it.
     */
    fun <T : Any> hasBehavior(type: Class<T>): Boolean =
        behaviors.any { type.isAssignableFrom(it::class.java) }
    
    /**
     * Checks whether this [NovaItem] has the specific [behavior] instance.
     */
    fun hasBehavior(behavior: ItemBehavior): Boolean =
        behaviors.contains(behavior)
    
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
     * Gets the first [ItemBehavior] that is an instance of [type] or a subclass, or null if there is none.
     */
    fun <T : Any> getBehaviorOrNull(type: Class<T>): T? =
        behaviors.firstOrNull { type.isAssignableFrom(it::class.java) } as T?
    
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
     * Gets the first [ItemBehavior] that is an instance of [behavior], or throws an [IllegalStateException] if there is none.
     */
    fun <T : Any> getBehavior(behavior: Class<T>): T =
        getBehaviorOrNull(behavior) ?: throw IllegalStateException("Item $id does not have a behavior of type ${behavior.simpleName}")
    
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
        behaviors.fold(damage) { currentDamage, behavior ->
            behavior.modifyBlockDamage(player, itemStack.clone(), block, currentDamage)
        }
    }
    
    /**
     * Modifies the client-side item type of this [NovaItem], in the context that it is sent to [player] and has [server] data.
     */
    fun modifyClientSideItemType(
        player: Player?,
        server: ItemStack,
        client: Material
    ): Material = runSafely("modify client-side item type", client, allowOffMain = true) {
        behaviors.fold(client) { current, behavior -> behavior.modifyClientSideItemType(player, server.clone(), current) }
    }
    
    /**
     * Modifies the client-side stack of this [NovaItem], in the context that it is sent to [player] and has [data].
     */
    fun modifyClientSideStack(
        player: Player?,
        server: ItemStack,
        client: ItemStack
    ): ItemStack = runSafely("modify client-side stack", client, allowOffMain = true) {
        behaviors.fold(client) { stack, behavior -> behavior.modifyClientSideStack(player, server.clone(), client.clone()) }
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
        for (behavior in behaviors) {
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
        val pos = nmsCtx.clickedPos.toNovaPos(nmsCtx.level.world)
        val face = nmsCtx.clickedFace.blockFace
        
        if (player is Player && !ProtectionManager.canUseBlock(player, itemStack, pos))
            return NmsInteractionResult.FAIL
        
        val ctx = Context.intention(BlockInteract)
            .param(BlockInteract.BLOCK_POS, pos)
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
        for (behavior in behaviors) {
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
        for (behavior in behaviors) {
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
        behaviors.forEach { it.handleAttackEntity(player, itemStack.clone(), attacked, event) }
    }
    
    /**
     * Handles [event] where [player] breaks a block using [itemStack] with this [NovaItem].
     */
    fun handleBreakBlock(
        player: Player,
        itemStack: ItemStack,
        event: BlockBreakEvent
    ): Unit = runSafely("handle break block") {
        behaviors.forEach { it.handleBreakBlock(player, itemStack.clone(), event) }
    }
    
    /**
     * Handles [event] where [player] damages [itemStack] with this [NovaItem].
     */
    fun handleDamage(
        player: Player,
        itemStack: ItemStack,
        event: PlayerItemDamageEvent
    ): Unit = runSafely("handle damage") {
        behaviors.forEach { it.handleDamage(player, itemStack.clone(), event) }
    }
    
    /**
     * Handles [event] where [player] breaks [itemStack] with this [NovaItem].
     */
    fun handleBreak(
        player: Player,
        itemStack: ItemStack,
        event: PlayerItemBreakEvent
    ): Unit = runSafely("handle break") {
        behaviors.forEach { it.handleBreak(player, itemStack.clone(), event) }
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
        behaviors.forEach { it.handleEquip(player, itemStack.clone(), slot, equipped, event) }
    }
    
    /**
     * Handles [event] where [player] clicks on [itemStack] with this [NovaItem] in an inventory.
     */
    fun handleInventoryClick(
        player: Player,
        itemStack: ItemStack,
        event: InventoryClickEvent
    ): Unit = runSafely("handle inventory click") {
        behaviors.forEach { it.handleInventoryClick(player, itemStack.clone(), event) }
    }
    
    /**
     * Handles [event] where [player] clicks on a slot with [itemStack] with this [NovaItem] on their cursor.
     */
    fun handleInventoryClickOnCursor(
        player: Player,
        itemStack: ItemStack,
        event: InventoryClickEvent
    ): Unit = runSafely("handle inventory click on cursor") {
        behaviors.forEach { it.handleInventoryClickOnCursor(player, itemStack.clone(), event) }
    }
    
    /**
     * Handles [event] where [player] swaps [itemStack] with this [NovaItem] via hotbar swap.
     */
    fun handleInventoryHotbarSwap(
        player: Player,
        itemStack: ItemStack,
        event: InventoryClickEvent
    ): Unit = runSafely("handle inventory hotbar swap") {
        behaviors.forEach { it.handleInventoryHotbarSwap(player, itemStack.clone(), event) }
    }
    
    /**
     * Handles [event] where [player] performs a block break action using [itemStack] with this [NovaItem].
     */
    fun handleBlockBreakAction(
        player: Player,
        itemStack: ItemStack,
        event: BlockBreakActionEvent
    ): Unit = runSafely("handle block break action") {
        behaviors.forEach { it.handleBlockBreakAction(player, itemStack.clone(), event) }
    }
    
    /**
     * Handles [event] where [player] consumes [itemStack] with this [NovaItem].
     */
    fun handleConsume(
        player: Player,
        itemStack: ItemStack,
        event: PlayerItemConsumeEvent
    ): Unit = runSafely("handle consume") {
        behaviors.forEach { it.handleConsume(player, itemStack.clone(), event) }
    }
    
    /**
     * Handles an inventory tick for [player] with [itemStack] with this [NovaItem] in [slot].
     */
    fun handleInventoryTick(
        player: Player,
        itemStack: ItemStack,
        slot: Int
    ): Unit = runSafely("handle inventory tick") {
        behaviors.forEach { it.handleInventoryTick(player, itemStack.clone(), slot) }
    }
    
    /**
     * Handles an equipment tick for [player] with [itemStack] with this [NovaItem] in [slot].
     */
    fun handleEquipmentTick(
        player: Player,
        itemStack: ItemStack,
        slot: EquipmentSlot
    ): Unit = runSafely("handle equipment tick") {
        behaviors.forEach { it.handleEquipmentTick(player, itemStack.clone(), slot) }
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
        behaviors.forEach { it.handleUseTick(entity, itemStack.clone(), hand, remainingUseTicks) }
    }
    
    /**
     * Handles the use of [itemStack] with this [NovaItem] finishing for [entity] in [hand].
     */
    fun handleUseFinished(
        entity: LivingEntity,
        itemStack: ItemStack,
        hand: EquipmentSlot,
    ): Unit = runSafely("handle use finished") {
        behaviors.forEach { it.handleUseFinished(entity, itemStack.clone(), hand) }
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
        behaviors.forEach { it.handleUseStopped(entity, itemStack.clone(), hand, remainingUseTicks) }
    }
    
    /**
     * Modifies the [remainder] item stack of using [original] with this [NovaItem] for [entity] in [hand].
     */
    fun modifyUseRemainder(
        entity: LivingEntity,
        original: ItemStack,
        hand: EquipmentSlot,
        remainder: ItemStack
    ): ItemStack = runSafely("modify use remainder", remainder) {
        behaviors.fold(remainder) { currentRemainder, behavior ->
            behavior.modifyUseRemainder(entity, original.clone(), hand, currentRemainder)
        }.clone()
    }
    
    /**
     * Modifies the use [duration] of [itemStack] with this [NovaItem] for [entity].
     */
    fun modifyUseDuration(
        entity: LivingEntity,
        itemStack: ItemStack,
        duration: Int
    ): Int = runSafely("modify use duration", duration) {
        behaviors.fold(duration) { currentDuration, behavior ->
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
            LOGGER.error("Failed to $name for $id", t)
        }
        return fallback
    }
    
    //</editor-fold>
    
    override fun toString() = id.toString()
    
    companion object {
        
        val CODEC = NovaRegistries.ITEM.byNameCodec()
        
    }
    
}