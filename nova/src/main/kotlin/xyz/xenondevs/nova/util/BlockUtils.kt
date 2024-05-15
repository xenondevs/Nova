package xyz.xenondevs.nova.util

import net.kyori.adventure.text.Component
import net.minecraft.core.Direction
import net.minecraft.core.Holder
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
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
import org.bukkit.block.Block
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
import org.bukkit.craftbukkit.v1_20_R3.event.CraftEventFactory
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockExpEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nmsutils.particle.block
import xyz.xenondevs.nmsutils.particle.particle
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.DefaultContextIntentions.BlockBreak
import xyz.xenondevs.nova.data.context.intention.DefaultContextIntentions.BlockPlace
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.util.data.getOrNull
import xyz.xenondevs.nova.util.item.hasNoBreakParticles
import xyz.xenondevs.nova.util.item.playPlaceSoundEffect
import xyz.xenondevs.nova.util.item.soundGroup
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.behavior.BlockSounds
import xyz.xenondevs.nova.world.block.behavior.Breakable
import xyz.xenondevs.nova.world.block.logic.`break`.BlockBreaking
import xyz.xenondevs.nova.world.block.logic.sound.SoundEngine
import xyz.xenondevs.nova.world.block.sound.SoundGroup
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.model.BackingStateBlockModelProvider
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.world.pos
import java.util.*
import kotlin.math.floor
import kotlin.random.Random
import net.minecraft.core.BlockPos as MojangBlockPos
import net.minecraft.world.entity.player.Player as MojangPlayer
import net.minecraft.world.item.ItemStack as MojangStack
import net.minecraft.world.level.block.Block as MojangBlock

/**
 * The [ResourceLocation] of this block, considering blocks from Nova, custom item services and vanilla.
 */
val Block.id: ResourceLocation
    get() = WorldDataManager.getBlockState(pos)?.block?.id
        ?: CustomItemServiceManager.getId(this)?.let { ResourceLocation.of(it, ':') }
        ?: ResourceLocation("minecraft", type.name.lowercase())

/**
 * The [NovaBlockState] at the position of this [Block].
 */
val Block.novaBlockState: NovaBlockState?
    get() = WorldDataManager.getBlockState(pos)

/**
 * The [NovaBlock] of the [NovaBlockState] at the position of this [Block].
 */
val Block.novaBlock: NovaBlock?
    get() = novaBlockState?.block

/**
 * The hardness of this block, also considering the custom hardness of Nova blocks.
 */
val Block.hardness: Double
    get() {
        val novaBlock = WorldDataManager.getBlockState(pos)?.block
        if (novaBlock != null) {
            val breakable = novaBlock.getBehaviorOrNull<Breakable>()
            return breakable?.hardness ?: -1.0
        } else {
            return type.hardness.toDouble()
        }
    }

/**
 * The sound group of this block, also considering custom sound groups of Nova blocks.
 */
val Block.novaSoundGroup: SoundGroup?
    get() {
        val novaBlock = novaBlock
        if (novaBlock != null) {
            return novaBlock.getBehaviorOrNull<BlockSounds>()?.soundGroup
        }
        
        return SoundGroup.from(type.soundGroup)
    }

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
 * The location at the center of this block.
 */
val Block.center: Location
    get() = Location(world, x + 0.5, y + 0.5, z + 0.5)

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
    val novaBlockState = WorldDataManager.getBlockState(pos)
    if (novaBlockState != null) {
        BlockBreaking.setBreakStage(pos, entityId, stage)
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
    val packet = ClientboundBlockDestructionPacket(entityId, location.blockPos, stage)
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
    val packet = ClientboundBlockDestructionPacket(player.entityId, location.blockPos, stage)
    MINECRAFT_SERVER.playerList.broadcast(player, location, 32.0, packet)
}

/**
 * Sends the [ClientboundLevelEventPacket] to all players in a 1-chunk-range,
 * causing break particles and sounds to be played. Only works with vanilla blocks.
 */
fun Block.broadcastBreakEvent() {
    val packet = ClientboundLevelEventPacket(2001, pos.nmsPos, MojangBlock.getId(nmsState), false)
    MINECRAFT_SERVER.playerList.broadcast(null as MojangPlayer?, this, 64.0, packet)
}

object BlockUtils {
    
    /**
     * Changes the state of the custom Nova block at [pos] to [blockState].
     * 
     * @throws IllegalArgumentException If there is no custom Nova block of the same block type at [pos].
     * For such cases, use [breakBlock] and [placeBlock] instead.
     */
    fun updateBlockState(pos: BlockPos, blockState: NovaBlockState) {
        val prevBlockState = WorldDataManager.getBlockState(pos)
        if (prevBlockState == blockState)
            return
        
        require(prevBlockState != null && prevBlockState.block == blockState.block) { "New block state needs to be of the same block type" }
        
        blockState.modelProvider.replace(pos, prevBlockState.modelProvider)
        WorldDataManager.setBlockState(pos, blockState)
    }
    
    /**
     * Places a block using the given [Context].
     *
     * Works for vanilla blocks, Nova blocks and blocks from custom item integrations.
     *
     * @param ctx The context to use
     * @return If a block has been placed
     */
    fun placeBlock(ctx: Context<BlockPlace>): Boolean {
        val pos = ctx[DefaultContextParamTypes.BLOCK_POS]!!
        val novaBlockState = ctx[DefaultContextParamTypes.BLOCK_STATE_NOVA]
        if (novaBlockState != null) {
            placeNovaBlock(pos, novaBlockState, ctx)
            return true
        }
        
        // TODO: place block by block state / id
        val itemStack: ItemStack? = ctx[DefaultContextParamTypes.BLOCK_ITEM_STACK]
        val placeEffects = ctx[DefaultContextParamTypes.BLOCK_PLACE_EFFECTS]
        if (itemStack != null) {
            if (CustomItemServiceManager.placeBlock(itemStack, pos.location, placeEffects))
                return true
            
            if (itemStack.type.isBlock) {
                val fakePlayer = EntityUtils.createFakePlayer(
                    ctx[DefaultContextParamTypes.SOURCE_LOCATION] ?: pos.location,
                    UUID.randomUUID(), ""
                )
                return placeVanillaBlock(pos, fakePlayer, itemStack, placeEffects)
            }
        }
        
        return false
    }
    
    internal fun placeNovaBlock(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockPlace>) {
        val block = state.block
        WorldDataManager.setBlockState(pos, state)
        block.handlePlace(pos, state, ctx)
        
        // sounds
        if (ctx[DefaultContextParamTypes.BLOCK_PLACE_EFFECTS]) {
            val soundGroup = block.getBehaviorOrNull<BlockSounds>()?.soundGroup
            if (soundGroup != null) {
                pos.playSound(soundGroup.placeSound, soundGroup.placeVolume, soundGroup.placePitch)
            }
        }
    }
    
    /**
     * Places the [itemStack] at the position of this Block
     *
     * @param player The [Player] to be used for place checking
     * @param itemStack The [ItemStack] to be placed
     * @param placeEffects If the place effects should be played
     * @return If the item could be placed
     */
    internal fun placeVanillaBlock(pos: BlockPos, player: ServerPlayer, itemStack: ItemStack, placeEffects: Boolean): Boolean {
        val nmsStack = itemStack.nmsCopy
        val blockItem = nmsStack.item as BlockItem
        val result = blockItem.place(BlockPlaceContext(UseOnContext(
            pos.world.serverLevel,
            player,
            InteractionHand.MAIN_HAND,
            nmsStack,
            BlockHitResult(
                Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()),
                Direction.UP,
                pos.nmsPos,
                false
            )
        )))
        
        if (result.consumesAction()) {
            setBlockEntityDataFromItemStack(pos, itemStack)
            if (placeEffects) itemStack.type.playPlaceSoundEffect(pos.location)
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
    private fun setBlockEntityDataFromItemStack(pos: BlockPos, itemStack: ItemStack) {
        val tileEntityTag = itemStack.nmsVersion.tag
            ?.getOrNull<CompoundTag>("BlockEntityTag") // ?.let { if (it.isEmpty) itemTag else it } TODO ?
            ?: return
        
        pos.world.serverLevel.getBlockEntity(pos.nmsPos, true)?.load(tileEntityTag)
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
        val pos = ctx[DefaultContextParamTypes.BLOCK_POS]!!
        val items = breakBlockInternal(ctx, sendEffectsToBreaker = true)
        
        val player = ctx[DefaultContextParamTypes.SOURCE_ENTITY] as? Player ?: return
        val block = pos.block
        CraftEventFactory.handleBlockDropItemEvent(
            block, block.state,
            player.serverPlayer,
            EntityUtils.createBlockDropItemEntities(pos, items)
        )
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
    
    internal fun breakBlockInternal(ctx: Context<BlockBreak>, sendEffectsToBreaker: Boolean): List<ItemStack> {
        val pos = ctx[DefaultContextParamTypes.BLOCK_POS]!!
        val bukkitBlock = pos.block
        val breakEffects = ctx[DefaultContextParamTypes.BLOCK_BREAK_EFFECTS]
        val drops = ctx[DefaultContextParamTypes.BLOCK_DROPS]
        
        if (CustomItemServiceManager.getId(bukkitBlock) != null) {
            val itemDrops = if (drops)
                CustomItemServiceManager.getDrops(bukkitBlock, ctx[DefaultContextParamTypes.TOOL_ITEM_STACK])!!
            else emptyList()
            CustomItemServiceManager.removeBlock(bukkitBlock, breakEffects)
            return itemDrops
        }
        
        val novaBlockState = WorldDataManager.getBlockState(pos)
        if (novaBlockState != null) {
            val itemDrops = if (drops)
                novaBlockState.block.getDrops(pos, novaBlockState, ctx)
            else emptyList()
            breakNovaBlockInternal(ctx, sendEffectsToBreaker)
            return itemDrops
        }
        
        val nmsPlayer = ctx[DefaultContextParamTypes.SOURCE_ENTITY]?.nmsEntity as? ServerPlayer ?: EntityUtils.DUMMY_PLAYER
        val tool = ctx[DefaultContextParamTypes.TOOL_ITEM_STACK]
        return breakVanillaBlock(pos, nmsPlayer, tool, drops, breakEffects, sendEffectsToBreaker)
    }
    
    internal fun breakNovaBlockInternal(ctx: Context<BlockBreak>, sendEffectsToBreaker: Boolean): Boolean {
        val pos = ctx[DefaultContextParamTypes.BLOCK_POS]!!
        val state = WorldDataManager.getBlockState(pos)
            ?: return false
        
        if (ctx[DefaultContextParamTypes.BLOCK_BREAK_EFFECTS]) {
            playBreakEffects(state, ctx, pos, sendEffectsToBreaker)
        }
        
        WorldDataManager.setBlockState(pos, null)
        state.block.handleBreak(pos, state, ctx)
        
        return true
    }
    
    private fun playBreakEffects(state: NovaBlockState, ctx: Context<BlockBreak>, pos: BlockPos, sendEffectsToBreaker: Boolean) {
        val player = ctx[DefaultContextParamTypes.SOURCE_ENTITY] as? Player
        val level = pos.world.serverLevel
        val dimension = level.dimension()
        val nmsPos = pos.nmsPos
        
        fun broadcast(packet: Packet<*>, sendEffectsToBreaker: Boolean) {
            MINECRAFT_SERVER.playerList.broadcast(
                if (sendEffectsToBreaker) null else player?.serverPlayer,
                pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
                64.0,
                dimension,
                packet
            )
        }
        
        fun broadcastBreakSound(soundGroup: SoundGroup) {
            val soundPacket = ClientboundSoundPacket(
                Holder.direct(SoundEvent.createVariableRangeEvent(ResourceLocation(soundGroup.breakSound))),
                SoundSource.BLOCKS,
                nmsPos.x + 0.5,
                nmsPos.y + 0.5,
                nmsPos.z + 0.5,
                soundGroup.breakVolume,
                soundGroup.breakPitch,
                Random.nextLong()
            )
            
            broadcast(soundPacket, true)
        }
        
        val soundGroup = state.block.getBehaviorOrNull<BlockSounds>()?.soundGroup
        if (state.modelProvider.provider == BackingStateBlockModelProvider) {
            // use the level event packet for blocks that use block states
            val levelEventPacket = ClientboundLevelEventPacket(2001, nmsPos, pos.nmsBlockState.id, false)
            broadcast(levelEventPacket, sendEffectsToBreaker)
            
            if (soundGroup != null && SoundEngine.overridesSound(pos.block.type.soundGroup.breakSound.key.key))
                broadcastBreakSound(soundGroup)
        } else {
            // send sound and break particles manually for display entity blocks
            if (soundGroup != null)
                broadcastBreakSound(soundGroup)
            
            val breakParticlesMaterial = state.block.getBehaviorOrNull<Breakable>()?.breakParticles
            if (breakParticlesMaterial != null) {
                val breakParticles = particle(ParticleTypes.BLOCK, pos.location.add(0.5, 0.5, 0.5)) {
                    block(breakParticlesMaterial)
                    offset(0.3, 0.3, 0.3)
                    amount(70)
                }
                broadcast(breakParticles, sendEffectsToBreaker || pos.block.type.hasNoBreakParticles())
            }
        }
    }
    
    internal fun breakVanillaBlock(
        pos: BlockPos,
        player: ServerPlayer,
        tool: ItemStack?,
        drops: Boolean,
        breakEffects: Boolean,
        sendEffectsToBreaker: Boolean
    ): List<ItemStack> {
        val level = pos.world.serverLevel
        val nmsPos = pos.nmsPos
        val state = pos.nmsBlockState
        val block = state.block
        
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
            
            val removed = level.removeBlock(nmsPos, false)
            if (removed) {
                block.destroy(level, nmsPos, state)
                
                if (drops && !player.isCreative) {
                    block.playerDestroy(level, player, nmsPos, state, level.getBlockEntity(nmsPos), tool.nmsCopy)
                }
            }
        }.map { it.item.bukkitMirror }
    }
    
    /**
     * Gets a list of [ItemStacks][ItemStack] containing the drops of this [Block] for the specified [ctx].
     *
     * Works for vanilla blocks, Nova blocks and blocks from custom item integrations.
     */
    fun getDrops(ctx: Context<BlockBreak>): List<ItemStack> {
        val pos = ctx[DefaultContextParamTypes.BLOCK_POS]!!
        val tool = ctx[DefaultContextParamTypes.TOOL_ITEM_STACK]
        
        val block = pos.block
        if (CustomItemServiceManager.getBlockType(block) != null)
            return CustomItemServiceManager.getDrops(block, tool) ?: emptyList()
        
        val novaBlockState = WorldDataManager.getBlockState(pos)
        if (novaBlockState != null)
            return novaBlockState.block.getDrops(pos, novaBlockState, ctx)
        
        return getVanillaDrops(pos, tool, ctx[DefaultContextParamTypes.SOURCE_ENTITY])
    }
    
    private fun getVanillaDrops(pos: BlockPos, tool: ItemStack?, sourceEntity: Entity?): List<ItemStack> {
        val drops = ArrayList<ItemStack>()
        val block = pos.block
        val state = block.state
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
        if (sourceEntity !is Player || sourceEntity.gameMode != GameMode.CREATIVE) {
            val mainBlock = block.getMainHalf()
            drops += if (tool != null && sourceEntity != null)
                mainBlock.getDrops(tool, sourceEntity)
            else mainBlock.getDrops(tool)
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
    fun getExp(ctx: Context<BlockBreak>): Int {
        val pos = ctx[DefaultContextParamTypes.BLOCK_POS]!!
        val novaState = WorldDataManager.getBlockState(pos)
        if (novaState != null)
            return novaState.block.getExp(pos, novaState, ctx)
        
        val serverLevel = pos.world.serverLevel
        val mojangPos = pos.nmsPos
        
        val toolItemStack = ctx[DefaultContextParamTypes.TOOL_ITEM_STACK].nmsCopy
        var exp = BlockUtils.getVanillaBlockExp(serverLevel, mojangPos, toolItemStack)
        
        // the furnace is the only block entity that can drop exp (I think)
        val furnace = serverLevel.getBlockEntity(mojangPos) as? AbstractFurnaceBlockEntity
        if (furnace != null) {
            exp += BlockUtils.getVanillaFurnaceExp(furnace)
        }
        
        return exp
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
    
    /**
     * Gets the name of [block] as a [Component]. Works for Nova blocks, custom item services and vanilla blocks.
     */
    fun getName(block: Block): Component {
        return CustomItemServiceManager.getName(block, "en_us")
            ?: WorldDataManager.getBlockState(block.pos)?.block?.name
            ?: Component.translatable(block.type.nmsBlock.descriptionId)
    }
    
    /**
     * Checks if a block is blocked by the hitbox of an entity.
     */
    internal fun isUnobstructed(pos: BlockPos, entity: Entity?, blockData: BlockData): Boolean {
        val context = entity?.let { CollisionContext.of(entity.nmsEntity) } ?: CollisionContext.empty()
        return pos.world.serverLevel.isUnobstructed(blockData.nmsBlockState, pos.nmsPos, context)
    }
    
}