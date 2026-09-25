package xyz.xenondevs.nova.util

import io.papermc.paper.math.BlockPosition
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.util.ProblemReporter
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.item.crafting.AbstractCookingRecipe
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.TallFlowerBlock
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.world.level.storage.TagValueInput
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Campfire
import org.bukkit.block.Chest
import org.bukkit.block.Container
import org.bukkit.block.Jukebox
import org.bukkit.block.Lectern
import org.bukkit.block.ShulkerBox
import org.bukkit.block.data.Bisected
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.Bed
import org.bukkit.block.data.type.PistonHead
import org.bukkit.craftbukkit.event.CraftEventFactory
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockExpEvent
import org.bukkit.inventory.ItemStack
import org.joml.Vector3i
import org.joml.primitives.AABBd
import org.joml.primitives.AABBf
import org.joml.primitives.AABBi
import xyz.xenondevs.commons.math.insecureRandomUuid
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockBreak
import xyz.xenondevs.nova.context.intention.BlockPlace
import xyz.xenondevs.nova.context.intention.ImplicitIntentions
import xyz.xenondevs.nova.util.item.playPlaceSoundEffect
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.particle.block
import xyz.xenondevs.nova.util.particle.particle
import xyz.xenondevs.nova.world.block.NovaBlockState
import xyz.xenondevs.nova.world.block.blockType
import xyz.xenondevs.nova.world.block.isNova
import xyz.xenondevs.nova.world.block.logic.`break`.BlockBreaking
import xyz.xenondevs.nova.world.block.logic.sound.SoundEngine
import xyz.xenondevs.nova.world.block.novaBlock
import xyz.xenondevs.nova.world.block.sound.SoundGroup
import xyz.xenondevs.nova.world.block.state.model.BackingStateBlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.ModelLessBlockModelProvider
import xyz.xenondevs.nova.world.item.itemType
import kotlin.math.floor
import kotlin.random.Random
import net.minecraft.core.BlockPos as MojangBlockPos
import net.minecraft.world.entity.player.Player as MojangPlayer
import net.minecraft.world.item.ItemStack as MojangStack
import net.minecraft.world.level.block.Block as MojangBlock

// TODO: doc    

fun BlockPosition.toBlock(world: World): Block =
    world.getBlockAt(blockX(), blockY(), blockZ())

/**
 * The block that is one y-level above the current one.
 */
val Block.above: Block
    get() = world.getBlockAt(x, y + 1, z)

/**
 * The block that is one y-level below the current one.
 */
val Block.below: Block
    get() = world.getBlockAt(x, y - 1, z)

/**
 * Schedules a tick at [this] block in [delay] ticks.
 * Canceled if the block's type changes.
 */
fun Block.scheduleTick(delay: Int) {
    require(delay >= 0) { "Delay must not be negative" }
    world.serverLevel.scheduleTick(nmsPos, nmsBlockState.block, delay)
}

/**
 * The location at the center of this block.
 */
val Block.center: Location
    get() = Location(world, x + 0.5, y + 0.5, z + 0.5)

/**
 * The sound group of this block. Supports custom sound groups.
 */
val Block.novaSoundGroup: SoundGroup
    get() = SoundGroup.from(nmsBlockState.soundType)

/**
 * The water color of the biome at this block's position.
 */
val Block.waterColor: Color
    get() = Color.fromARGB(world.serverLevel.getBiome(nmsPos).value().waterColor)

@Deprecated("Use Bukkit equivalent", ReplaceWith("getRelative(x, y, z)"))
fun Block.add(x: Int, y: Int, z: Int): Block =
    getRelative(x, y, z)

@Deprecated("Use Bukkit equivalent", ReplaceWith("getRelative(face, step)"))
fun Block.advance(face: BlockFace, step: Int = 1): Block =
    getRelative(face, step)

fun Block.playSound(sound: String, volume: Float, pitch: Float) {
    world.playSound(Location(world, x + .5, y + .5, z + .5), sound, volume, pitch)
}

fun Block.playSound(sound: String, category: SoundCategory, volume: Float, pitch: Float) {
    world.playSound(Location(world, x + .5, y + .5, z + .5), sound, category, volume, pitch)
}

fun Block.playSound(sound: Sound, volume: Float, pitch: Float) {
    world.playSound(Location(world, x + .5, y + .5, z + .5), sound, volume, pitch)
}

/**
 * Converts this [Block's][Block] position to a [Vector3i].
 */
fun Block.toVector3i(): Vector3i = Vector3i(x, y, z)

/**
 * Converts this [Block] to an [AABBi] from `(x, y, z)` to `(x + 1, y + 1, z + 1)`.
 */
fun Block.toAABBi() = AABBi(x, y, z, x + 1, y + 1, z + 1)

/**
 * Converts this [Block] to an [AABBd] from `(x, y, z)` to `(x + 1, y + 1, z + 1)`.
 */
fun Block.toAABBd() = AABBd(x.toDouble(), y.toDouble(), z.toDouble(), x + 1.0, y + 1.0, z + 1.0)

/**
 * Converts this [Block] to an [AABBf] from `(x, y, z)` to `(x + 1, y + 1, z + 1)`.
 */
fun Block.toAABBf() = AABBf(x.toFloat(), y.toFloat(), z.toFloat(), x + 1.0f, y + 1.0f, z + 1.0f)

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
    val type = blockType
    if (type.isNova) {
        BlockBreaking.setBreakStage(this, entityId, stage)
    } else {
        broadcastDestructionStage(entityId, stage)
    }
}

/**
 * Sends the [ClientboundBlockDestructionPacket] to all players in a 1-chunk-range
 * with the given [entityId] and breaking [stage]. Only works with vanilla blocks.
 *
 * @param entityId The id of the entity breaking the block
 * @param stage The breaking stage between 0-9 (both inclusive).
 * A different number will cause the breaking texture to disappear.
 *
 * @see Block.setBreakStage
 */
fun Block.broadcastDestructionStage(entityId: Int, stage: Int) {
    val packet = ClientboundBlockDestructionPacket(entityId, location.Block, stage)
    MINECRAFT_SERVER.playerList.broadcast(location, 32.0, packet)
}

/**
 * Sends the [ClientboundBlockDestructionPacket] to all players in a 1-chunk-range
 * with the entity id of the given [player] and breaking [stage]. Only works with vanilla blocks.
 *
 * @param player The player breaking the block. The packet will not be sent to this player.
 * @param stage The breaking stage between 0-9 (both inclusive).
 * A different number will cause the breaking texture to disappear.
 *
 * @see Block.setBreakStage
 */
fun Block.broadcastDestructionStage(player: Player, stage: Int) {
    val packet = ClientboundBlockDestructionPacket(player.entityId, location.Block, stage)
    MINECRAFT_SERVER.playerList.broadcast(player, location, 32.0, packet)
}

/**
 * Sends the [ClientboundLevelEventPacket] to all players in a 1-chunk-range,
 * causing break particles and sounds to be played. Only works with vanilla blocks.
 */
fun Block.broadcastBreakEvent() {
    val packet = ClientboundLevelEventPacket(2001, nmsPos, MojangBlock.getId(nmsBlockState), false)
    MINECRAFT_SERVER.playerList.broadcast(null as MojangPlayer?, this, 64.0, packet)
}

object BlockUtils {
    
    /**
     * Places a block using the given [Context].
     *
     * Works for vanilla blocks, Nova blocks and blocks from custom item integrations.
     *
     * @param ctx The context to use
     * @return If a block has been placed
     */
    fun placeBlock(ctx: Context<BlockPlace>): Boolean = ScopedValue.where(ImplicitIntentions.BLOCK_PLACE, ctx).exec {
        val block = ctx[BlockPlace.BLOCK]
        val state = ctx[BlockPlace.BLOCK_STATE]
        if (state is NovaBlockState) {
            val flags = ctx[BlockPlace.BLOCK_UPDATE_FLAGS]
            val level = block.world.serverLevel
            level.setBlock(block.nmsPos, state.nmsBlockState, flags.value)
            
            if (ctx[BlockPlace.BLOCK_PLACE_EFFECTS])
                block.novaSoundGroup.playPlaceSound(block)
            
            return@exec true
        } else {
            // TODO: place block by block state
            // TODO: respect block update flags
            val itemStack: ItemStack? = ctx[BlockPlace.BLOCK_ITEM_STACK]
            val placeEffects = ctx[BlockPlace.BLOCK_PLACE_EFFECTS]
            if (itemStack != null && itemStack.itemType.hasBlockType()) {
                val fakePlayer = EntityUtils.createFakePlayer(
                    ctx[BlockPlace.SOURCE_LOCATION] ?: block.location,
                    insecureRandomUuid(), ""
                )
                
                return@exec placeVanillaBlock(
                    block,
                    ctx[BlockPlace.CLICKED_BLOCK_FACE] ?: BlockFace.UP,
                    fakePlayer,
                    itemStack,
                    placeEffects
                )
            }
        }
        
        return@exec false
    }
    
    
    /**
     * Places the [itemStack] at the position of this Block
     *
     * @param player The [Player] to be used for place checking
     * @param itemStack The [ItemStack] to be placed
     * @param placeEffects If the place effects should be played
     * @return If the item could be placed
     */
    internal fun placeVanillaBlock(block: Block, clickedFace: BlockFace, player: ServerPlayer, itemStack: ItemStack, placeEffects: Boolean): Boolean {
        val nmsStack = itemStack.unwrap().copy()
        val blockItem = nmsStack.item as BlockItem
        val result = blockItem.place(BlockPlaceContext(UseOnContext(
            block.world.serverLevel,
            player,
            InteractionHand.MAIN_HAND,
            nmsStack,
            BlockHitResult(
                Vec3(block.x.toDouble(), block.y.toDouble(), block.z.toDouble()),
                clickedFace.nmsDirection,
                block.nmsPos,
                false
            )
        )))
        
        if (result.consumesAction()) {
            setBlockEntityDataFromItemStack(block, itemStack)
            if (placeEffects) itemStack.type.playPlaceSoundEffect(block.location)
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
    private fun setBlockEntityDataFromItemStack(block: Block, itemStack: ItemStack) {
        val tileEntityTag = itemStack.unwrap().get(DataComponents.BLOCK_ENTITY_DATA)?.copyTagWithBlockEntityId()
            ?: return
        
        val input = TagValueInput.create(ProblemReporter.DISCARDING, block.world.serverLevel.registryAccess(), tileEntityTag)
        block.world.serverLevel.getBlockEntity(block.nmsPos)?.loadWithComponents(input)
    }
    
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
    fun breakBlockNaturally(ctx: Context<BlockBreak>) {
        val block = ctx[BlockBreak.BLOCK]
        val items = breakBlockInternal(ctx, sendEffectsToBreaker = true)
        
        val player = ctx[BlockBreak.SOURCE_ENTITY] as? Player
        val itemEntities = EntityUtils.createBlockDropItemEntities(block, items)
        if (player != null) {
            CraftEventFactory.handleBlockDropItemEvent(block, block.state, player.serverPlayer, itemEntities)
        } else {
            itemEntities.forEach(block.world.serverLevel::addFreshEntity)
        }
    }
    
    /**
     * Breaks this block using the given [ctx] and returns the drops.
     *
     * This method works for vanilla blocks, blocks from Nova and blocks from custom item services.
     * Items will **not** be dropped in the world, but instead returned as a list. The drops
     * depend on the source and tool defined in the [ctx]. If the source is a player, it will be
     * as if the player broke the block. The tool item stack will not be damaged.
     *
     * @param ctx The [Context] to be used
     */
    fun breakBlock(ctx: Context<BlockBreak>): List<ItemStack> {
        return breakBlockInternal(ctx, true)
    }
    
    internal fun breakBlockInternal(ctx: Context<BlockBreak>, sendEffectsToBreaker: Boolean): List<ItemStack> = ScopedValue.where(ImplicitIntentions.BLOCK_BREAK, ctx).exec {
        val block = ctx[BlockBreak.BLOCK]
        val blockState = ctx[BlockBreak.BLOCK_STATE]
        if (blockState is NovaBlockState) {
            val novaBlock = blockState.novaBlock
            val drops = novaBlock.getDrops(block, blockState, ctx)
            val level = block.world.serverLevel
            val pos = block.nmsPos
            
            if (ctx[BlockBreak.BLOCK_BREAK_EFFECTS]) {
                playBreakEffects(
                    blockState,
                    block,
                    if (sendEffectsToBreaker) null else ctx[BlockBreak.SOURCE_ENTITY] as? Player
                )
            }
            
            level.setBlock(pos, level.getFluidState(pos).createLegacyBlock(), ctx[BlockBreak.BLOCK_UPDATE_FLAGS].value)
            return@exec drops
        } else {
            return@exec breakVanillaBlock(
                block,
                ctx[BlockBreak.SOURCE_ENTITY]?.nmsEntity as? ServerPlayer ?: EntityUtils.DUMMY_PLAYER,
                ctx[BlockBreak.TOOL_ITEM_STACK],
                ctx[BlockBreak.BLOCK_DROPS],
                ctx[BlockBreak.BLOCK_BREAK_EFFECTS],
                sendEffectsToBreaker
            )
        }
    }
    
    internal fun playBreakEffects(state: NovaBlockState, block: Block, excludedPlayer: Player?) {
        val level = block.world.serverLevel
        val dimension = level.dimension()
        val nmsPos = block.nmsPos
        
        fun broadcast(packet: Packet<*>, excludedPlayer: Player? = null) {
            MINECRAFT_SERVER.playerList.broadcast(
                excludedPlayer?.serverPlayer,
                block.x.toDouble(), block.y.toDouble(), block.z.toDouble(),
                64.0,
                dimension,
                packet
            )
        }
        
        fun broadcastBreakSound(soundGroup: SoundGroup) {
            val soundPacket = ClientboundSoundPacket(
                Holder.direct(soundGroup.nmsSoundType.breakSound),
                SoundSource.BLOCKS,
                nmsPos.x + 0.5,
                nmsPos.y + 0.5,
                nmsPos.z + 0.5,
                soundGroup.breakVolume,
                soundGroup.breakPitch,
                Random.nextLong()
            )
            
            broadcast(soundPacket)
        }
        
        fun broadcastCustomBreakParticles(includeExcludedPlayer: Boolean) {
            val breakParticlesBlock = state.novaBlock.breakParticles
                ?: return
            val breakParticles = particle(ParticleTypes.BLOCK, block.location.add(0.5, 0.5, 0.5)) {
                block(breakParticlesBlock)
                offset(0.3, 0.3, 0.3)
                amount(70)
            }
            broadcast(breakParticles, if (includeExcludedPlayer) null else excludedPlayer)
        }
        
        val soundGroup = SoundGroup.from(state.nmsBlockState.soundType)
        val modelProvider = state.novaBlock.modelProviders.get()[state.nmsBlockState]
            ?: return
        val clientsideBlockState = modelProvider.clientsideBlockState
        val hasNoBreakParticles = clientsideBlockState.renderShape == RenderShape.INVISIBLE || !clientsideBlockState.shouldSpawnTerrainParticles()
        if (modelProvider is BackingStateBlockModelProvider || modelProvider is ModelLessBlockModelProvider) {
            // use the level event packet for blocks that use block states (sound & particles)
            val levelEventPacket = ClientboundLevelEventPacket(2001, nmsPos, state.nmsBlockState.id, false)
            broadcast(levelEventPacket, excludedPlayer)
            
            if (SoundEngine.overridesSound(clientsideBlockState.soundType.breakSound))
                broadcastBreakSound(soundGroup)
            
            // if no break particles were displayed with the level event packet, send custom ones
            if (modelProvider is ModelLessBlockModelProvider && hasNoBreakParticles)
                broadcastCustomBreakParticles(true)
        } else if (modelProvider is DisplayEntityBlockModelProvider) {
            // send sound and break particles manually for display entity blocks
            broadcastBreakSound(soundGroup)
            broadcastCustomBreakParticles(hasNoBreakParticles)
        }
    }
    
    internal fun breakVanillaBlock(
        block: Block,
        player: ServerPlayer,
        tool: ItemStack?,
        drops: Boolean,
        breakEffects: Boolean,
        sendEffectsToBreaker: Boolean
    ): List<ItemStack> {
        val level = block.world.serverLevel
        val nmsPos = block.nmsPos
        val state = block.nmsBlockState
        val block = state.block
        
        if (state.isAir)
            return emptyList()
        
        return level.captureDrops {
            // calls game and level events (includes break effects), angers piglins, ignites unstable tnt, etc.
            val willDestroy = { block.playerWillDestroy(level, nmsPos, state, player); Unit }
            if (breakEffects) {
                if (sendEffectsToBreaker) {
                    forcePacketBroadcast(willDestroy)
                } else willDestroy()
            } else {
                preventPacketBroadcast(willDestroy)
            }
            
            val blockEntity = level.getBlockEntity(nmsPos)
            val removed = level.removeBlock(nmsPos, false)
            if (removed) {
                block.destroy(level, nmsPos, state)
                
                if (!player.isCreative) {
                    block.playerDestroy(level, player, nmsPos, state, blockEntity, tool.unwrap().copy(), drops, false)
                }
            }
        }.map { it.item.asBukkitMirror() }
    }
    
    /**
     * Gets a list of [ItemStacks][ItemStack] containing the drops of this [Block] for the specified [ctx].
     */
    fun getDrops(ctx: Context<BlockBreak>): List<ItemStack> {
        val pos = ctx[BlockBreak.BLOCK]
        val state = ctx[BlockBreak.BLOCK_STATE]
        val tool = ctx[BlockBreak.TOOL_ITEM_STACK]
        
        if (state is NovaBlockState)
            return state.novaBlock.getDrops(pos, state, ctx)
        
        val drops = ArrayList<ItemStack>()
        if (ctx[BlockBreak.BLOCK_STORAGE_DROPS]) {
            // note: storage drops in nms are implemented via BlockEntity#preRemoveSideEffects
            when (state) {
                is Chest ->
                    drops += state.blockInventory.contents.asSequence().filterNotNull().map(ItemStack::clone)
                
                is Container if state !is ShulkerBox ->
                    drops += state.inventory.contents.asSequence().filterNotNull().map(ItemStack::clone)
                
                is Lectern ->
                    drops += state.inventory.contents.asSequence().filterNotNull().map(ItemStack::clone)
                
                is Jukebox ->
                    state.record.takeUnlessEmpty()?.clone()?.also(drops::add)
                
                is Campfire ->
                    repeat(4) { state.getItem(it)?.clone()?.also(drops::add) }
            }
        }
        
        if (ctx[BlockBreak.BLOCK_DROPS]) {
            val mainPos = pos.getMainHalf()
            val builder = LootParams.Builder(mainPos.world.serverLevel)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(mainPos.nmsPos))
                .withParameter(LootContextParams.TOOL, tool.unwrap())
                .withOptionalParameter(LootContextParams.THIS_ENTITY, ctx[BlockBreak.SOURCE_ENTITY]?.nmsEntity)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, mainPos.nmsBlockEntity);
            drops += mainPos.nmsBlockState.getDrops(builder).map { it.asBukkitMirror() }
        }
        
        return drops.filterNot { it.isEmpty }
    }
    
    private fun Block.getMainHalf(): Block {
        val data = blockData
        val nmsBlock = nmsBlockState.block
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
     * Gets the experience that would be dropped if the block were to be broken with [ctx].
     */
    fun getExp(ctx: Context<BlockBreak>): Int {
        val pos = ctx[BlockBreak.BLOCK]
        val state = ctx[BlockBreak.BLOCK_STATE]
        if (state is NovaBlockState)
            return state.novaBlock.getExp(pos, state, ctx)
        
        val serverLevel = pos.world.serverLevel
        val mojangPos = pos.nmsPos
        
        val toolItemStack = ctx[BlockBreak.TOOL_ITEM_STACK].unwrap().copy()
        var exp = getVanillaBlockExp(serverLevel, mojangPos, toolItemStack)
        
        // the furnace is the only block entity that can drop exp (I think)
        val furnace = serverLevel.getBlockEntity(mojangPos) as? AbstractFurnaceBlockEntity
        if (furnace != null) {
            exp += getVanillaFurnaceExp(furnace)
        }
        
        return exp
    }
    
    internal fun getVanillaBlockExp(level: ServerLevel, pos: MojangBlockPos, tool: MojangStack): Int {
        val blockState = level.getBlockState(pos)
        val block = blockState.block
        return block.getExpDrop(blockState, level, pos, tool, true)
    }
    
    internal fun getVanillaFurnaceExp(furnace: AbstractFurnaceBlockEntity): Int {
        return furnace.recipesUsed.reference2IntEntrySet().sumOf { entry ->
            val recipeHolder = MINECRAFT_SERVER.recipeManager.byKey(entry.key).orElse(null)
            val recipe = recipeHolder?.value as? AbstractCookingRecipe
            
            val amount = entry.intValue
            val expPerRecipe = recipe?.experience()?.toDouble() ?: 0.0
            
            // Minecraft's logic to calculate the furnace exp
            var exp = floor(amount * expPerRecipe).toInt()
            val f = (amount * expPerRecipe) % 1
            if (f != 0.0 && Math.random() < f) {
                exp++
            }
            
            return@sumOf exp
        }
    }
    
    /**
     * Checks if a block is blocked by the hitbox of an entity.
     */
    internal fun isUnobstructed(block: Block, entity: Entity?, blockData: BlockData): Boolean {
        val context = entity?.let { CollisionContext.of(entity.nmsEntity) } ?: CollisionContext.empty()
        return block.world.serverLevel.isUnobstructed(blockData.nmsBlockState, block.nmsPos, context)
    }
    
    internal fun broadcastBlockUpdate(block: Block) {
        val level = block.world.serverLevel
        val nmsPos = block.nmsPos
        val nmsState = block.nmsBlockState
        level.notifyAndUpdatePhysics(nmsPos, level.getChunkAt(nmsPos), nmsState, nmsState, nmsState, 3, 512)
    }
    
}
