package xyz.xenondevs.nova.util

import net.kyori.adventure.text.Component
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.item.crafting.AbstractCookingRecipe
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.TallFlowerBlock
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Campfire
import org.bukkit.block.Chest
import org.bukkit.block.Container
import org.bukkit.block.Jukebox
import org.bukkit.block.Lectern
import org.bukkit.block.ShulkerBox
import org.bukkit.block.data.Bisected
import org.bukkit.block.data.Levelled
import org.bukkit.block.data.type.Bed
import org.bukkit.block.data.type.PistonHead
import org.bukkit.craftbukkit.v1_20_R2.event.CraftEventFactory
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockExpEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nmsutils.particle.block
import xyz.xenondevs.nmsutils.particle.particle
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockBreak
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockPlace
import xyz.xenondevs.nova.data.context.param.ContextParamTypes
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.util.data.getOrNull
import xyz.xenondevs.nova.util.item.playPlaceSoundEffect
import xyz.xenondevs.nova.util.item.soundGroup
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.limits.TileEntityLimits
import xyz.xenondevs.nova.world.block.logic.`break`.BlockBreaking
import xyz.xenondevs.nova.world.block.sound.SoundGroup
import xyz.xenondevs.nova.world.pos
import java.util.*
import kotlin.math.floor
import kotlin.random.Random
import net.minecraft.core.BlockPos as MojangBlockPos
import net.minecraft.world.entity.player.Player as MojangPlayer
import net.minecraft.world.item.ItemStack as MojangStack
import net.minecraft.world.item.context.BlockPlaceContext as MojangBlockPlaceContext
import net.minecraft.world.level.block.Block as MojangBlock

// region block info

/**
 * The [ResourceLocation] of this block, considering blocks from Nova, custom item services and vanilla.
 */
val Block.id: ResourceLocation
    get() = BlockManager.getBlockState(pos)?.id
        ?: CustomItemServiceManager.getId(this)?.let { ResourceLocation.of(it, ':') }
        ?: ResourceLocation("minecraft", type.name.lowercase())

/**
 * The [NovaBlockState] at the position of this [Block].
 */
val Block.novaBlockState: NovaBlockState?
    get() = BlockManager.getBlockState(pos)

/**
 * The [NovaBlock] of the [NovaBlockState] at the position of this [Block].
 */
val Block.novaBlock: NovaBlock?
    get() = BlockManager.getBlockState(pos)?.block

/**
 * The block that is one y-level below the current one.
 */
val Block.below: Block
    get() = world.getBlockAt(x, y - 1, z)

/**
 * The block that is one y-level above the current one.
 */
val Block.above: Block
    get() = world.getBlockAt(x, y + 1, z)

/**
 * The location at the center of this block.
 */
val Block.center: Location
    get() = Location(world, x + 0.5, y + 0.5, z + 0.5)

/**
 * The hardness of this block, also considering the custom hardness of Nova blocks.
 */
val Block.hardness: Double
    get() = BlockManager.getBlockState(pos)?.block?.options?.hardness ?: type.hardness.toDouble()

/**
 * The break texture for this block, also considering custom break textures of Nova blocks.
 */
val Block.breakTexture: Material
    get() {
        val novaBlock = novaBlock
        if (novaBlock != null) {
            return novaBlock.options.breakParticles ?: Material.AIR
        }
        
        return type
    }

/**
 * The sound group of this block, also considering custom sound groups of Nova blocks.
 */
val Block.novaSoundGroup: SoundGroup?
    get() {
        val novaBlock = novaBlock
        if (novaBlock != null) {
            return novaBlock.options.soundGroup
        }
        
        return SoundGroup.from(type.soundGroup)
    }

/**
 * Checks if the block below has the same [type][Block.getType].
 */
fun Block.hasSameTypeBelow(): Boolean {
    return type == below.type
}

/**
 * Checks if this block is a source fluid.
 */
fun Block.isSourceFluid(): Boolean {
    return blockData is Levelled && (blockData as Levelled).level == 0
}

/**
 * Gets the fluid type of this block or null if it's not a fluid.
 */
val Block.sourceFluidType: FluidType?
    get() {
        val blockData = blockData
        if (blockData is Levelled && blockData.level == 0) {
            return when (type) {
                Material.WATER, Material.BUBBLE_COLUMN -> FluidType.WATER
                Material.LAVA -> FluidType.LAVA
                else -> null
            }
        }
        
        return null
    }

// endregion

// region block placing

/**
 * Places a block using the given [BlockPlaceContext].
 *
 * Works for vanilla blocks, Nova blocks and blocks from custom item integrations.
 *
 * @param ctx The context to use
 * @param playSound If the block placing sound should be played
 * @return If a block has been placed
 */
fun Block.place(ctx: Context<BlockPlace>): Boolean {
    val novaBlock: NovaBlock? = ctx[ContextParamTypes.BLOCK_TYPE_NOVA]
    if (novaBlock != null) {
        if (novaBlock is NovaTileEntityBlock && !TileEntityLimits.canPlace(ctx).allowed)
            return false
        
        BlockManager.placeBlockState(novaBlock, ctx)
        return true
    }
    
    // TODO: place block by blockstate / id
    val itemStack: ItemStack? = ctx[ContextParamTypes.BLOCK_ITEM_STACK]
    if (itemStack != null) {
        if (CustomItemServiceManager.placeBlock(itemStack, ctx[ContextParamTypes.BLOCK_POS]!!.location, ctx[ContextParamTypes.BLOCK_PLACE_EFFECTS]))
            return true
        
        if (itemStack.type.isBlock) {
            val fakePlayer = EntityUtils.createFakePlayer(ctx[ContextParamTypes.SOURCE_LOCATION] ?: location, UUID.randomUUID(), "")
            return placeVanilla(fakePlayer, itemStack)
        }
    }
    
    return false
}

/**
 * Places the [itemStack] at the position of this Block
 *
 * @param player The [Player] to be used for place checking
 * @param itemStack The [ItemStack] to be placed
 * @param playSound If the block placing sound should be played
 * @return If the item could be placed
 */
internal fun Block.placeVanilla(player: ServerPlayer, itemStack: ItemStack, playSound: Boolean = true): Boolean {
    val location = location
    val nmsStack = itemStack.nmsCopy
    val blockItem = nmsStack.item as BlockItem
    val result = blockItem.place(MojangBlockPlaceContext(UseOnContext(
        world.serverLevel,
        player,
        InteractionHand.MAIN_HAND,
        nmsStack,
        BlockHitResult(
            Vec3(location.x, location.y, location.z),
            Direction.UP,
            MojangBlockPos(location.blockX, location.blockY, location.blockZ),
            false
        )
    )))
    
    if (result.consumesAction()) {
        setBlockEntityDataFromItemStack(itemStack)
        if (playSound) itemStack.type.playPlaceSoundEffect(location)
        return true
    }
    return false
}

/**
 * Loads the "BlockEntityTag" to this BlockEntity if the tag is present on the [ItemStack]
 * and this [Block] is a BlockEntity.
 * (Example: A chest item with stored items inside)
 *
 * @param itemStack The [ItemStack] to load the data from
 */
private fun Block.setBlockEntityDataFromItemStack(itemStack: ItemStack) {
    val itemTag = CompoundTag()
    ReflectionRegistry.CB_CRAFT_META_APPLY_TO_ITEM_METHOD.invoke(itemStack.itemMeta, itemTag)
    
    val tileEntityTag = itemTag.getOrNull<CompoundTag>("BlockEntityTag")?.let { if (it.isEmpty) itemTag else it }
    if (tileEntityTag != null) {
        val world = this.world.serverLevel
        world.getBlockEntity(MojangBlockPos(x, y, z), true)?.load(tileEntityTag)
    }
}

/**
 * Checks if a block is blocked by the hitbox of an entity.
 */
fun Block.isUnobstructed(player: Entity? = null, blockType: Material = this.type): Boolean {
    require(blockType.isBlock) { "Material must be a block" }
    val level = world.serverLevel
    val context = player?.let { CollisionContext.of(it.nmsEntity) } ?: CollisionContext.empty()
    return level.isUnobstructed(blockType.nmsBlock.defaultBlockState(), pos.nmsPos, context)
}

// endregion

// region block breaking

/**
 * Breaks this block naturally using the given [ctx].
 *
 * This method works for vanilla blocks, blocks from Nova and blocks from custom item integrations.
 * Items will be dropped in the world, those drops depend on the source and tool defined in the [ctx].
 * If the source is a player, it will be as if the player broke the block.
 * The tool item stack will not be damaged.
 *
 * @param ctx The [Context] to be used
 */
fun Block.breakNaturally(ctx: Context<BlockBreak>) {
    val state = state
    val itemEntities = removeInternal(ctx, sendEffectsToBreaker = true)
    val player = ctx[ContextParamTypes.SOURCE_ENTITY] as? Player ?: return
    CraftEventFactory.handleBlockDropItemEvent(this, state, player.serverPlayer, itemEntities)
}

/**
 * Removes this block using the given [ctx].
 *
 * This method works for vanilla blocks, blocks from Nova and blocks from custom item services.
 *
 * @param ctx The [Context] to be used
 */
fun Block.remove(ctx: Context<BlockBreak>): List<ItemStack> {
    return removeInternal(ctx, true).map { it.item.bukkitMirror }
}

internal fun Block.removeInternal(ctx: Context<BlockBreak>, sendEffectsToBreaker: Boolean): List<ItemEntity> {
    val breakEffects = ctx[ContextParamTypes.BLOCK_BREAK_EFFECTS]
    val drops = ctx[ContextParamTypes.BLOCK_DROPS]
    
    if (CustomItemServiceManager.getId(this) != null) {
        val itemEntities = CustomItemServiceManager.getDrops(this, ctx[ContextParamTypes.TOOL_ITEM_STACK])!!.let(::createDroppedItemEntities)
        CustomItemServiceManager.removeBlock(this, breakEffects)
        return itemEntities
    }
    
    if (BlockManager.getBlockState(pos) != null) {
        val itemEntities = if (drops) BlockManager.getDrops(ctx)!!.let(::createDroppedItemEntities) else emptyList()
        BlockManager.removeBlockStateInternal(ctx, sendEffectsToBreaker)
        return itemEntities
    }
    
    val nmsPlayer: ServerPlayer = ctx[ContextParamTypes.SOURCE_ENTITY]?.nmsEntity as? ServerPlayer ?: EntityUtils.DUMMY_PLAYER
    val level = world.serverLevel
    val pos = pos.nmsPos
    val state = nmsState
    val block = state.block
    val blockEntity = level.getBlockEntity(pos)
    
    return level.captureDrops {
        // calls game and level events (includes break effects), angers piglins, ignites unstable tnt, etc.
        val willDestroy = { block.playerWillDestroy(level, pos, state, nmsPlayer) }
        if (breakEffects) {
            if (sendEffectsToBreaker) {
                forcePacketBroadcast(willDestroy)
            } else willDestroy()
        } else {
            preventPacketBroadcast(willDestroy)
        }
        
        val removed = level.removeBlock(pos, false)
        if (removed) {
            block.destroy(level, pos, state)
            
            if (ctx[ContextParamTypes.BLOCK_DROPS]) {
                block.playerDestroy(level, nmsPlayer, pos, state, blockEntity, ctx[ContextParamTypes.TOOL_ITEM_STACK].nmsCopy)
            }
        }
    }
}

internal fun Block.createDroppedItemEntities(items: Iterable<ItemStack>): List<ItemEntity> {
    return items.map {
        ItemEntity(
            world.serverLevel,
            x + 0.5 + Random.nextDouble(-0.25, 0.25),
            y + 0.5 + Random.nextDouble(-0.25, 0.25),
            z + 0.5 + Random.nextDouble(-0.25, 0.25),
            it.nmsCopy
        ).apply(ItemEntity::setDefaultPickUpDelay)
    }
}

/**
 * Gets a list of [ItemStacks][ItemStack] containing the drops of this [Block] for the specified [BlockBreakContext].
 *
 * Works for vanilla blocks, Nova blocks and blocks from custom item integrations.
 */
fun Block.getAllDrops(ctx: Context<BlockBreak>): List<ItemStack> {
    val tool: ItemStack? = ctx[ContextParamTypes.TOOL_ITEM_STACK]
    CustomItemServiceManager.getDrops(this, tool)?.let { return it }
    
    val novaBlockState = BlockManager.getBlockState(pos)
    if (novaBlockState != null)
        return novaBlockState.block.logic.getDrops(novaBlockState, ctx)
    
    val drops = ArrayList<ItemStack>()
    val state = state
    when {
        state is Chest ->
            drops += state.blockInventory.contents.asSequence().filterNotNull().map(ItemStack::clone)
        
        state is Container && state !is ShulkerBox ->
            drops += state.inventory.contents.asSequence().filterNotNull().map(ItemStack::clone)
        
        state is Lectern ->
            drops += state.inventory.contents.asSequence().filterNotNull().map(ItemStack::clone)
        
        state is Jukebox ->
            state.record.takeUnlessEmpty()?.clone()?.also(drops::add)
        
        state is Campfire ->
            repeat(4) { state.getItem(it)?.clone()?.also(drops::add) }
    }
    
    // don't include the actual block for creative players
    val sourceEntity: Entity? = ctx[ContextParamTypes.SOURCE_ENTITY]
    if (sourceEntity !is Player || sourceEntity.gameMode != GameMode.CREATIVE) {
        val block = getMainHalf()
        drops += if (tool != null && sourceEntity != null)
            block.getDrops(tool, sourceEntity)
        else block.getDrops(tool)
    }
    
    return drops.filterNot { it.type.isAir }
}

private fun Block.getMainHalf(): Block {
    val data = blockData
    val nmsBlock = type.nmsBlock
    if (nmsBlock is TallFlowerBlock || nmsBlock is DoorBlock) { // 2 block tall
        data as Bisected
        if (data.half == Bisected.Half.TOP) {
            return location.subtract(0.0, 1.0, 0.0).block
        }
    } else if (data is Bed) {
        if (data.part == Bed.Part.FOOT) {
            return location.advance(data.facing).block
        }
    } else if (data is PistonHead) {
        return location.advance(data.facing.oppositeFace).block
    }
    
    return this
}

/**
 * Gets the experience that would be dropped if the block were to be broken.
 */
fun Block.getExp(ctx: Context<BlockBreak>): Int {
    val pos: BlockPos = ctx[ContextParamTypes.BLOCK_POS]!!
    val novaState = BlockManager.getBlockState(pos)
    if (novaState != null)
        return novaState.block.logic.getExp(novaState, ctx)
    
    val serverLevel = pos.world.serverLevel
    val mojangPos = pos.nmsPos
    
    val toolItemStack = ctx[ContextParamTypes.TOOL_ITEM_STACK].nmsCopy
    var exp = BlockUtils.getVanillaBlockExp(serverLevel, mojangPos, toolItemStack)
    
    // the furnace is the only block entity that can drop exp (I think)
    val furnace = serverLevel.getBlockEntity(mojangPos) as? AbstractFurnaceBlockEntity
    if (furnace != null) {
        exp += BlockUtils.getVanillaFurnaceExp(furnace)
    }
    
    return exp
}

/**
 * Spawns an experience orb of [exp] from this block after calling the [BlockExpEvent].
 * @return The amount of exp that was actually spawned
 */
fun Block.spawnExpOrb(exp: Int, location: Location = this.location.add(.5, .5, .5)): Int {
    val event = BlockExpEvent(this, exp).also(::callEvent)
    if (event.expToDrop > 0) {
        ExperienceOrb.award(location.world!!.serverLevel, Vec3(location.x, location.y, location.z), event.expToDrop)
        return event.expToDrop
    }
    
    return 0
}
// endregion

// region block effects

/**
 * Displays the break particles and plays the break sound for this [Block].
 * Only works with vanilla blocks.
 */
fun Block.playBreakEffects() {
    val packet = ClientboundLevelEventPacket(2001, pos.nmsPos, MojangBlock.getId(nmsState), false)
    MINECRAFT_SERVER.playerList.broadcast(null as MojangPlayer?, this, 64.0, packet)
}

/**
 * Displays the break particles for this block.
 * Only works with vanilla blocks.
 */
@Suppress("DeprecatedCallableAddReplaceWith", "DEPRECATION")
@Deprecated("Not real break particles. Consider using Block#playBreakEffects.")
fun Block.showBreakParticles() {
    type.showBreakParticles(this.location)
}

/**
 * Displays the break particles for this [Material] at the given [location].
 */
@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("Not real break particles. Consider using Block#playBreakEffects.")
fun Material.showBreakParticles(location: Location) {
    MINECRAFT_SERVER.playerList.broadcast(location, 64.0, getBreakParticlesPacket(location))
}

/**
 * Creates a [ClientboundLevelParticlesPacket] mimicking the particle effects of a block breaking.
 */
fun Material.getBreakParticlesPacket(location: Location): ClientboundLevelParticlesPacket {
    return particle(ParticleTypes.BLOCK, location.add(0.5, 0.5, 0.5)) {
        block(this@getBreakParticlesPacket)
        offset(0.3, 0.3, 0.3)
        amount(70)
    }
}

/**
 * Plays the breaking sound for this block.
 */
fun Block.playBreakSound() {
    val soundGroup = novaSoundGroup ?: return
    world.playSound(location, soundGroup.breakSound, soundGroup.breakVolume, soundGroup.breakPitch)
}

/**
 * Sets the break stage for this [Block].
 * Works with Nova and vanilla blocks.
 *
 * **It is required to reset the stage before removing the block!**
 *
 * @param entityId The id of the entity breaking the block
 * @param stage The breaking stage between 0-9 (both inclusive).
 * A different number will cause the breaking texture to disappear.
 */
fun Block.setBreakStage(entityId: Int, stage: Int) {
    val novaBlockState = BlockManager.getBlockState(pos)
    if (novaBlockState != null) {
        BlockBreaking.setBreakStage(pos, entityId, stage)
    } else {
        sendDestructionPacket(entityId, stage)
    }
}

/**
 * Sends the [ClientboundBlockDestructionPacket] to all players in a 1-chunk-range
 * with the given [entityId] and breaking [stage]. Might not work with some Nova blocks.
 *
 * @param entityId The id of the entity breaking the block
 * @param stage The breaking stage between 0-9 (both inclusive).
 * A different number will cause the breaking texture to disappear.
 *
 * @see Block.setBreakStage
 */
fun Block.sendDestructionPacket(entityId: Int, stage: Int) {
    val packet = ClientboundBlockDestructionPacket(entityId, location.blockPos, stage)
    MINECRAFT_SERVER.playerList.broadcast(location, 32.0, packet)
}

/**
 * Sends the [ClientboundBlockDestructionPacket] to all players in a 1-chunk-range
 * with the entity id of the given [player] and breaking [stage]. Might not work with some Nova blocks.
 *
 * @param player The player breaking the block. The packet will not be sent to this player.
 * @param stage The breaking stage between 0-9 (both inclusive).
 * A different number will cause the breaking texture to disappear.
 *
 * @see Block.setBreakStage
 */
fun Block.sendDestructionPacket(player: Player, stage: Int) {
    val packet = ClientboundBlockDestructionPacket(player.entityId, location.blockPos, stage)
    MINECRAFT_SERVER.playerList.broadcast(player, location, 32.0, packet)
}

// endregion

object BlockUtils {
    
    fun getName(block: Block): Component {
        return CustomItemServiceManager.getName(block, "en_us")
            ?: BlockManager.getBlockState(block.pos)?.block?.name
            ?: Component.translatable(block.type.nmsBlock.descriptionId)
    }
    
    internal fun getVanillaBlockExp(level: ServerLevel, pos: MojangBlockPos, tool: MojangStack): Int {
        val blockState = level.getBlockState(pos)
        val block = blockState.block
        return block.getExpDrop(blockState, level, pos, tool, true)
    }
    
    internal fun getVanillaFurnaceExp(furnace: AbstractFurnaceBlockEntity): Int {
        return furnace.recipesUsed.object2IntEntrySet().sumOf { entry ->
            val recipeHolder = MINECRAFT_SERVER.recipeManager.byKey(entry.key).orElse(null)
            val recipe = recipeHolder?.value as? AbstractCookingRecipe
            
            val amount = entry.intValue
            val expPerRecipe = recipe?.experience?.toDouble() ?: 0.0
            
            // Minecraft's logic to calculate the furnace exp
            var exp = floor(amount * expPerRecipe).toInt()
            val f = (amount * expPerRecipe) % 1
            if (f != 0.0 && Math.random() < f) {
                exp++
            }
            
            return@sumOf exp
        }
    }
    
}