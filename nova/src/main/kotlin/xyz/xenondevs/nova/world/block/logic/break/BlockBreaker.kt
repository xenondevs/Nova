package xyz.xenondevs.nova.world.block.logic.`break`

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.world.phys.Vec3
import org.bukkit.Axis
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.SoundCategory
import org.bukkit.block.BlockFace
import org.bukkit.craftbukkit.event.CraftEventFactory
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockExpEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.DefaultContextIntentions.BlockBreak
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.util.BlockFaceUtils
import xyz.xenondevs.nova.util.BlockUtils
import xyz.xenondevs.nova.util.EntityUtils
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.axis
import xyz.xenondevs.nova.util.callEvent
import xyz.xenondevs.nova.util.damageToolBreakBlock
import xyz.xenondevs.nova.util.hardness
import xyz.xenondevs.nova.util.item.ToolUtils
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.novaSoundGroup
import xyz.xenondevs.nova.util.particle.item
import xyz.xenondevs.nova.util.particle.particle
import xyz.xenondevs.nova.util.send
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.util.serverTick
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.behavior.Breakable
import xyz.xenondevs.nova.world.block.event.BlockBreakActionEvent
import xyz.xenondevs.nova.world.block.sound.SoundGroup
import xyz.xenondevs.nova.world.block.state.NovaBlockState

internal class NovaBlockBreaker(
    player: Player,
    pos: BlockPos,
    val blockState: NovaBlockState,
    sequence: Int,
    blockedUntil: Int
) : BlockBreaker(player, pos, sequence, blockedUntil) {
    
    val blockType = blockState.block
    private val breakable = blockType.getBehavior<Breakable>()
    
    override fun createBreakMethod(): BreakMethod =
        BreakMethod.of(block, blockType, null)
    
    override fun handleBreakTick() {
        // spawn hit particles if not rendered clientside
        if (block.type == Material.BARRIER)
            spawnHitParticles()
    }
    
    private fun spawnHitParticles() {
        val texture = breakable.breakParticles ?: return
        val side = BlockFaceUtils.determineBlockFaceLookingAt(player.eyeLocation) ?: BlockFace.UP
        
        val particlePacket = particle(ParticleTypes.ITEM, block.location.add(0.5, 0.5, 0.5).advance(side, 0.6)) {
            Axis.entries.forEach { if (it != side.axis) offset(it, 0.2) }
            amount(1)
            speed(0f)
            item(texture)
        }
        
        player.send(particlePacket)
    }
    
}

internal class VanillaBlockBreaker(
    player: Player,
    pos: BlockPos,
    sequence: Int,
    blockedUntil: Int
) : BlockBreaker(player, pos, sequence, blockedUntil) {
    
    override fun createBreakMethod(): BreakMethod = PacketBreakMethod(pos)
    
    override fun handleBreakTick() = Unit
    
}

@Suppress("MemberVisibilityCanBePrivate")
internal sealed class BlockBreaker(val player: Player, val pos: BlockPos, val startSequence: Int, val blockedUntil: Int) {
    
    protected val breakMethod: BreakMethod by lazy { createBreakMethod() }
    
    val block = pos.block
    protected val soundGroup: SoundGroup? = block.novaSoundGroup
    protected val hardness: Double = block.hardness
    protected val tool: ItemStack? = player.inventory.itemInMainHand.takeUnlessEmpty()
    protected val itemToolCategories: Set<ToolCategory> = ToolCategory.ofItem(tool)
    protected val drops: Boolean = ToolUtils.isCorrectToolForDrops(block, tool)
    
    var destroyTicks = 0
        private set
    var progress = 0.0
        private set
    val isDone: Boolean
        get() = progress >= 1
    
    var isStopped: Boolean = false
        private set
    
    init {
        callEvent(BlockBreakActionEvent(player, block, BlockBreakActionEvent.Action.START))
    }
    
    fun handleTick() {
        if (isDone)
            return
        
        var damage = calculateDamage()
        val clientsideDamage = calculateClientsideDamage()
        
        if (clientsideDamage >= 1 && damage < 1) {
            stop(false, startSequence)
            return
        }
        
        val damageEvent = BlockDamageEvent(
            player,
            block,
            BlockFaceUtils.determineBlockFaceLookingAt(player.eyeLocation) ?: BlockFace.NORTH,
            tool ?: ItemStack(Material.AIR),
            damage > 1
        )
        callEvent(damageEvent)
        if (damageEvent.isCancelled)
            return
        if (damageEvent.instaBreak && damage < 1)
            damage = 1.0
        
        if (damage >= 1.0 || serverTick >= blockedUntil) {
            progress += damage
            
            // play break sound every 4 ticks
            if (progress < 1.0 && destroyTicks % 4 == 0 && soundGroup != null)
                pos.playSound(soundGroup.hitSound, SoundCategory.BLOCKS, soundGroup.hitVolume, soundGroup.hitPitch)
            
            destroyTicks++
        }
        
        // The block breaker will only initiate block removal for break processes that aren't predicted client-side.
        // If they are predicted client-side, the client will send a packet to the server to remove the block once it's done,
        // which is handled in BlockBreaking.kt. This then calls BlockBreaker#breakBlock and BlockBreaker#stop.
        if (isDone && !breakMethod.hasClientsidePrediction) {
            // Break block, call event, drop items and exp, etc.
            breakBlock(clientsideDamage >= 1, startSequence) // If the block broke instantaneously for the client, the effects will also be played clientside
            // Check if the breaker is still done. (This will not be the case when the BlockBreakEvent was cancelled)
            if (isDone) {
                // Stop break animation
                stop(true)
            }
        } else {
            // break tick logic of subclasses (i.e. spawning particles for barrier nova blocks)
            handleBreakTick()
            
            // set the break stage
            breakMethod.breakStage = (progress.coerceAtMost(1.0) * 10).toInt()
        }
    }
    
    fun breakBlock(brokenClientside: Boolean, sequence: Int) {
        // create a block breaking context
        val ctx = Context.intention(BlockBreak)
            .param(DefaultContextParamTypes.BLOCK_POS, pos)
            .param(DefaultContextParamTypes.SOURCE_ENTITY, player)
            .param(DefaultContextParamTypes.TOOL_ITEM_STACK, tool)
            .param(DefaultContextParamTypes.BLOCK_DROPS, drops)
        
        val level = block.world.serverLevel
        val blockPos = pos.nmsPos
        
        //<editor-fold desc="break event", defaultstate="collapsed">
        val event = BlockBreakEvent(block, player)
        if (drops) {
            event.expToDrop = when (this) {
                is NovaBlockBreaker -> blockType.getExp(pos, blockState, ctx.build())
                is VanillaBlockBreaker -> BlockUtils.getVanillaBlockExp(level, blockPos, tool.unwrap().copy())
            }
        }
        callEvent(event)
        ctx.param(DefaultContextParamTypes.BLOCK_DROPS, drops && event.isDropItems)
        //</editor-fold>
        
        if (!event.isCancelled && !ProtectionManager.isVanillaProtected(player, block.location)) {
            //<editor-fold desc="item drops", defaultstate="collapsed">
            //<editor-fold desc="exp drops", defaultstate="collapsed">
            if (level.gameRules.getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
                val exp = event.expToDrop
                if (exp > 0) {
                    ExperienceOrb.award(level, Vec3.atCenterOf(blockPos), event.expToDrop)
                }
            }
            
            // furnace experience has its own event
            val furnace = level.getBlockEntity(blockPos) as? AbstractFurnaceBlockEntity
            if (furnace != null) {
                val exp = BlockExpEvent(block, BlockUtils.getVanillaFurnaceExp(furnace))
                    .also(::callEvent)
                    .expToDrop
                
                if (exp > 0) {
                    // vanilla Minecraft does not check the block drops gamerule here, so we won't either
                    ExperienceOrb.award(level, Vec3.atCenterOf(blockPos), exp)
                }
            }
            //</editor-fold>
            
            // damage tool
            if (player.gameMode != GameMode.CREATIVE && itemToolCategories.isNotEmpty() && hardness > 0)
                player.damageToolBreakBlock()
            
            // capture previous state
            val state = block.state
            
            // remove block
            val items = BlockUtils.breakBlockInternal(ctx.build(), !brokenClientside)
            val itemEntities = EntityUtils.createBlockDropItemEntities(pos, items)
            
            // drop items
            if (event.isDropItems) {
                CraftEventFactory.handleBlockDropItemEvent(block, state, player.serverPlayer, itemEntities) // spawns item entities
            }
        } else {
            // If the block wasn't broken clientside, the client will keep breaking the block and not send
            // START_DESTROY_BLOCK again. For those cases, the internal progress will be reset as well.
            if (!brokenClientside)
                progress = 0.0
        }
        
        // send ack packet
        player.send(ClientboundBlockChangedAckPacket(sequence))
        // set cooldown for the next block
        BlockBreaking.setBreakCooldown(player)
    }
    
    fun stop(blockBroken: Boolean, sequence: Int? = null) {
        isStopped = true
        breakMethod.stop()
        
        if (sequence != null) {
            player.send(ClientboundBlockChangedAckPacket(sequence))
        }
        
        callEvent(BlockBreakActionEvent(player, block, if (blockBroken) BlockBreakActionEvent.Action.FINISH else BlockBreakActionEvent.Action.CANCEL))
    }
    
    private fun calculateClientsideDamage(): Double {
        return if (player.gameMode == GameMode.CREATIVE) 1.0 else 0.0
    }
    
    private fun calculateDamage(): Double {
        return ToolUtils.calculateDamage(player, block, tool)
    }
    
    protected abstract fun createBreakMethod(): BreakMethod
    protected abstract fun handleBreakTick()
    
}