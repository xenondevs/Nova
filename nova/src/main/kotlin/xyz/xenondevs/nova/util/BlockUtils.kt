package xyz.xenondevs.nova.util

import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
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
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockExpEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nmsutils.particle.block
import xyz.xenondevs.nmsutils.particle.particle
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.material.BlockNovaMaterial
import xyz.xenondevs.nova.material.TileEntityNovaMaterial
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.util.item.ToolUtils
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.item.playPlaceSoundEffect
import xyz.xenondevs.nova.util.item.soundGroup
import xyz.xenondevs.nova.util.item.takeUnlessAir
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
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
 * The [NamespacedId] of this block.
 */
val Block.id: NamespacedId
    get() {
        val pos = pos
        val novaMaterial = BlockManager.getBlock(pos)
        if (novaMaterial != null) {
            return novaMaterial.id
        }
        
        // TODO: Check CustomItemServiceManager
        
        return NamespacedId("minecraft", type.name.lowercase())
    }

/**
 * The [BlockNovaMaterial] of this block.
 */
val Block.novaMaterial: BlockNovaMaterial?
    get() = BlockManager.getBlock(pos)?.material

/**
 * The block that is one y-level below the current one.
 */
inline val Block.below: Block
    get() = world.getBlockAt(x, y - 1, z)

/**
 * The block that is one y-level above the current one.
 */
inline val Block.above: Block
    get() = world.getBlockAt(x, y + 1, z)

/**
 * The location at the center of this block.
 */
inline val Block.center: Location
    get() = Location(world, x + 0.5, y + 0.5, z + 0.5)

/**
 * The hardness of this block, also considering the custom hardness of Nova blocks.
 */
val Block.hardness: Double
    get() = BlockManager.getBlock(pos)?.material?.hardness ?: type.hardness.toDouble()

/**
 * The break texture for this block, also considering custom break textures of Nova blocks.
 */
val Block.breakTexture: Material
    get() = BlockManager.getBlock(pos)?.material?.breakParticles ?: type

/**
 * The sound group of this block, also considering custom sound groups of Nova blocks.
 */
val Block.soundGroup: SoundGroup?
    get() {
        val novaMaterial = BlockManager.getBlock(pos)?.material
        if (novaMaterial != null) {
            return novaMaterial.soundGroup
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
fun Block.place(ctx: BlockPlaceContext, playSound: Boolean = true): Boolean {
    val item = ctx.item
    val novaMaterial = item.novaMaterial
    if (novaMaterial is BlockNovaMaterial) {
        if (novaMaterial is TileEntityNovaMaterial && !TileEntityLimits.canPlace(ctx).allowed)
            return false
        
        BlockManager.placeBlock(novaMaterial, ctx, playSound)
        return true
    }
    
    if (CustomItemServiceManager.placeBlock(item, ctx.pos.location, playSound))
        return true
    
    if (item.type.isBlock) {
        val fakePlayer = EntityUtils.createFakePlayer(ctx.sourceLocation ?: location, UUID.randomUUID(), "")
        return placeVanilla(fakePlayer, item)
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
fun Block.placeVanilla(player: ServerPlayer, itemStack: ItemStack, playSound: Boolean = true): Boolean {
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
fun Block.setBlockEntityDataFromItemStack(itemStack: ItemStack) {
    val itemTag = CompoundTag()
    ReflectionRegistry.CB_CRAFT_META_APPLY_TO_METHOD.invoke(itemStack.itemMeta, itemTag)
    
    val tileEntityTag = itemTag.getCompound("BlockEntityTag")?.let { if (it.isEmpty) itemTag else it }
    if (tileEntityTag != null) {
        val world = this.world.serverLevel
        world.getBlockEntity(MojangBlockPos(x, y, z), true)?.load(tileEntityTag)
    }
}

/**
 * Checks if a block is blocked by the hitbox of an entity.
 */
fun Block.isUnobstructed(material: Material, player: Player? = null): Boolean {
    val level = world.serverLevel
    val context = player?.let { CollisionContext.of(it.nmsEntity) } ?: CollisionContext.empty()
    return level.isUnobstructed(material.nmsBlock.defaultBlockState(), pos.nmsPos, context)
}

// endregion

// region block breaking

/**
 * Removes this block using the given [ctx].
 *
 * This method works for vanilla blocks, blocks from Nova and blocks from custom item integrations.
 *
 * @param ctx The [BlockBreakContext] to be used
 * @param playSound If block breaking sounds should be played
 * @param showParticles If block break particles should be displayed
 */
@Deprecated("Break sound and particles are not independent from one another", ReplaceWith("remove(ctx, showParticles || playSound)"))
fun Block.remove(ctx: BlockBreakContext, playSound: Boolean = true, showParticles: Boolean = true) = remove(ctx, showParticles || playSound)

/**
 * Removes this block using the given [ctx].
 *
 * This method works for vanilla blocks, blocks from Nova and blocks from custom item services.
 *
 * @param ctx The [BlockBreakContext] to be used
 * @param breakEffects If break effects should be displayed (i.e. sounds and particle effects).
 * For vanilla blocks, this also includes some breaking logic (ice turning to water, unstable
 * tnt exploding, angering piglins, etc.)
 */
fun Block.remove(ctx: BlockBreakContext, breakEffects: Boolean) {
    removeInternal(ctx, ToolUtils.isCorrectToolForDrops(this, ctx.item), breakEffects, true)
}

internal fun Block.removeInternal(ctx: BlockBreakContext, drops: Boolean, breakEffects: Boolean, sendEffectsToBreaker: Boolean): List<ItemEntity> {
    if (CustomItemServiceManager.removeBlock(this, breakEffects, breakEffects))
        return CustomItemServiceManager.getDrops(this, ctx.item)!!.let(::dropItemsNaturallyInternal)
    if (BlockManager.removeBlockInternal(ctx, breakEffects, sendEffectsToBreaker))
        return BlockManager.getDrops(this.location, ctx.item)!!.let(::dropItemsNaturallyInternal)
    
    val nmsPlayer = (ctx.source as? Player)?.serverPlayer
        ?: ctx.source as? MojangPlayer
        ?: EntityUtils.DUMMY_PLAYER
    
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
            
            if (!nmsPlayer.isCreative && drops) {
                block.playerDestroy(level, nmsPlayer, pos, state, blockEntity, ctx.item.nmsCopy)
            }
        }
    }
}

internal fun Block.dropItemsNaturallyInternal(items: Iterable<ItemStack>): List<ItemEntity> {
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
fun Block.getAllDrops(ctx: BlockBreakContext): List<ItemStack> {
    val tool = ctx.item
    CustomItemServiceManager.getDrops(this, tool)?.let { return it }
    
    val novaBlockState = BlockManager.getBlock(pos)
    if (novaBlockState != null)
        return novaBlockState.material.novaBlock.getDrops(novaBlockState, ctx)
    
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
            state.record.takeUnlessAir()?.clone()?.also(drops::add)
        
        state is Campfire ->
            repeat(4) { state.getItem(it)?.clone()?.also(drops::add) }
    }
    
    // don't include the actual block for creative players
    if (ctx.source !is Player || ctx.source.gameMode != GameMode.CREATIVE) {
        val block = getMainHalf()
        drops += if (tool != null && ctx.source is Entity)
            block.getDrops(tool, ctx.source)
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
fun Block.getExp(ctx: BlockBreakContext): Int {
    val novaState = BlockManager.getBlock(ctx.pos)
    if (novaState != null)
        return novaState.material.novaBlock.getExp(novaState, ctx)
    
    val serverLevel = ctx.pos.world.serverLevel
    val mojangPos = ctx.pos.nmsPos
    
    var exp = BlockUtils.getVanillaBlockExp(serverLevel, mojangPos, ctx.item.nmsCopy)
    
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
    minecraftServer.playerList.broadcast(null as MojangPlayer?, this, 64.0, packet)
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
    minecraftServer.playerList.broadcast(location, 64.0, getBreakParticlesPacket(location))
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
    val soundGroup = soundGroup ?: return
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
    val novaBlockState = BlockManager.getBlock(pos)
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
    minecraftServer.playerList.broadcast(location, 32.0, packet)
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
    minecraftServer.playerList.broadcast(player, location, 32.0, packet)
}

// endregion

object BlockUtils {
    
    internal fun getVanillaBlockExp(level: ServerLevel, pos: MojangBlockPos, tool: MojangStack): Int {
        val blockState = level.getBlockState(pos) ?: return 0
        val block = blockState.block
        return block.getExpDrop(blockState, level, pos, tool, true)
    }
    
    internal fun getVanillaFurnaceExp(furnace: AbstractFurnaceBlockEntity): Int {
        return furnace.recipesUsed.object2IntEntrySet().sumOf { entry ->
            val amount = entry.intValue
            val expPerRecipe = (minecraftServer.recipeManager.byKey(entry.key).orElse(null) as? AbstractCookingRecipe)?.experience?.toDouble() ?: 0.0
            
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