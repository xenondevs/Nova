package xyz.xenondevs.nova.world.block.logic.`break`

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.world.phys.Vec3
import org.bukkit.Axis
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.SoundCategory
import org.bukkit.block.BlockFace
import org.bukkit.craftbukkit.v1_20_R3.event.CraftEventFactory
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExpEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import xyz.xenondevs.nmsutils.particle.item
import xyz.xenondevs.nmsutils.particle.particle
import xyz.xenondevs.nova.data.config.MAIN_CONFIG
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockBreak
import xyz.xenondevs.nova.data.context.param.ContextParamTypes
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.ToolTier
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
import xyz.xenondevs.nova.util.nmsCopy
import xyz.xenondevs.nova.util.nmsState
import xyz.xenondevs.nova.util.novaSoundGroup
import xyz.xenondevs.nova.util.send
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.util.serverTick
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.event.BlockBreakActionEvent
import xyz.xenondevs.nova.world.block.sound.SoundGroup
import xyz.xenondevs.nova.world.block.state.NovaBlockState

private val CLIENTSIDE_PREDICTIONS by MAIN_CONFIG.entry<Boolean>("world", "block_breaking", "clientside_predictions")
private val MINING_FATIGUE_INSTANCE = MobEffectInstance(MobEffects.DIG_SLOWDOWN, Integer.MAX_VALUE, 255, false, false, false)

internal class NovaBlockBreaker(
    player: Player,
    pos: BlockPos,
    val blockState: NovaBlockState,
    sequence: Int,
    blockedUntil: Int
) : BlockBreaker(player, pos, sequence, blockedUntil) {
    
    val blockType = blockState.block
    override val requiresToolForDrops: Boolean = blockType.options.requiresToolForDrops
    
    override fun createBreakMethod(clientsidePrediction: Boolean): BreakMethod =
        BreakMethod.of(block, blockType, if (clientsidePrediction) player else null)
    
    override fun handleBreakTick() {
        // spawn hit particles if not rendered clientside
        if (block.type == Material.BARRIER)
            spawnHitParticles()
    }
    
    private fun spawnHitParticles() {
        val texture = blockType.options.breakParticles ?: return
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
    
    override val requiresToolForDrops: Boolean = block.nmsState.requiresCorrectToolForDrops()
    
    override fun createBreakMethod(clientsidePrediction: Boolean): BreakMethod =
        if (clientsidePrediction)
            PacketBreakMethod(pos, player)
        else PacketBreakMethod(pos)
    
    override fun handleBreakTick() = Unit
    
}

@Suppress("MemberVisibilityCanBePrivate")
internal sealed class BlockBreaker(val player: Player, val pos: BlockPos, val startSequence: Int, val blockedUntil: Int) {
    
    protected val breakMethod: BreakMethod by lazy {
        val damage = calculateDamage()
        val clientsideDamage = calculateClientsideDamage()
        // Clientside predictions are turned off for blocks broken instantaneously, as there is no second packet being sent.
        createBreakMethod(CLIENTSIDE_PREDICTIONS && clientsideDamage == damage && damage < 1.0)
    }
    protected abstract val requiresToolForDrops: Boolean
    
    val block = pos.block
    protected val soundGroup: SoundGroup? = block.novaSoundGroup
    protected val hardness: Double = block.hardness
    protected val tool: ItemStack? = player.inventory.itemInMainHand.takeUnlessEmpty()
    protected val toolCategory: ToolCategory? = ToolCategory.ofItem(tool)
    protected val correctCategory: Boolean = toolCategory != null && toolCategory.isCorrectToolCategoryForBlock(block)
    protected val correctLevel: Boolean = ToolTier.isCorrectLevel(pos.block, tool)
    protected val drops: Boolean by lazy { !requiresToolForDrops || (correctCategory && correctLevel) } // lazy because accessing abstract val
    
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
        
        val damage = calculateDamage()
        val clientsideDamage = calculateClientsideDamage()
        
        if (clientsideDamage >= 1 && damage < 1) {
            stop(false, startSequence)
            return
        }
        
        if (damage >= 1.0 || serverTick >= blockedUntil) {
            progress += damage
            
            //<editor-fold desc="hit sounds", defaultstate="collapsed">
            if (progress < 1.0 && destroyTicks % 4 == 0) {
                if (soundGroup != null) {
                    pos.playSound(
                        soundGroup.hitSound,
                        SoundCategory.BLOCKS,
                        soundGroup.hitVolume,
                        soundGroup.hitPitch
                    )
                }
            }
            //</editor-fold>
            
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
                // Stop break animation and mining fatigue effect
                stop(true)
            }
        } else {
            // break tick logic of subclasses (i.e. spawning particles for barrier nova blocks)
            handleBreakTick()
            
            // set the break stage
            breakMethod.breakStage = (progress.coerceAtMost(1.0) * 10).toInt()
            
            if (!breakMethod.hasClientsidePrediction) {
                // re-send mining fatigue every tick to ensure that the player actually has it
                sendMiningFatigueEffect()
            }
        }
    }
    
    private fun sendMiningFatigueEffect() {
        val effect = player.getPotionEffect(PotionEffectType.SLOW_DIGGING)
        val packet = if (effect != null) {
            // The player might actually have mining fatigue.
            // In this case, it is important to copy the hasIcon value to prevent it from disappearing.
            val effectInstance = MobEffectInstance(
                MobEffects.DIG_SLOWDOWN,
                Int.MAX_VALUE, 255,
                effect.isAmbient, effect.hasParticles(), effect.hasIcon()
            )
            ClientboundUpdateMobEffectPacket(player.entityId, effectInstance)
        } else {
            // The player does not have mining fatigue, we can use the default effect instance
            ClientboundUpdateMobEffectPacket(player.entityId, MINING_FATIGUE_INSTANCE)
        }
        
        player.send(packet)
    }
    
    fun breakBlock(brokenClientside: Boolean, sequence: Int) {
        // create a block breaking context
        val ctx = Context.intention(BlockBreak)
            .param(ContextParamTypes.BLOCK_POS, pos)
            .param(ContextParamTypes.SOURCE_ENTITY, player)
            .param(ContextParamTypes.TOOL_ITEM_STACK, tool)
            .param(ContextParamTypes.BLOCK_DROPS, drops)
        
        val level = block.world.serverLevel
        val blockPos = pos.nmsPos
        
        //<editor-fold desc="break event", defaultstate="collapsed">
        val event = BlockBreakEvent(block, player)
        if (drops) {
            event.expToDrop = when (this) {
                is NovaBlockBreaker -> blockType.getExp(pos, blockState, ctx.build())
                is VanillaBlockBreaker -> BlockUtils.getVanillaBlockExp(level, blockPos, tool.nmsCopy)
            }
        }
        callEvent(event)
        ctx.param(ContextParamTypes.BLOCK_DROPS, drops && event.isDropItems)
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
            if (player.gameMode != GameMode.CREATIVE && toolCategory != null && hardness > 0)
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
        
        if (!breakMethod.hasClientsidePrediction) {
            val effect = player.getPotionEffect(PotionEffectType.SLOW_DIGGING)
            val effectPacket = if (effect != null) {
                // If the player actually has mining fatigue, send the correct effect again
                val effectInstance = MobEffectInstance(
                    MobEffects.DIG_SLOWDOWN,
                    effect.duration, effect.amplifier,
                    effect.isAmbient, effect.hasParticles(), effect.hasIcon()
                )
                ClientboundUpdateMobEffectPacket(player.entityId, effectInstance)
            } else {
                // Remove the effect
                ClientboundRemoveMobEffectPacket(player.entityId, MobEffects.DIG_SLOWDOWN)
            }
            
            player.send(effectPacket)
        }
        
        if (sequence != null) {
            player.send(ClientboundBlockChangedAckPacket(sequence))
        }
        
        callEvent(BlockBreakActionEvent(player, block, if (blockBroken) BlockBreakActionEvent.Action.FINISH else BlockBreakActionEvent.Action.CANCEL))
    }
    
    private fun calculateClientsideDamage(): Double {
        return ToolUtils.calculateDamageVanilla(player, block)
    }
    
    private fun calculateDamage(): Double {
        return ToolUtils.calculateDamage(player, block, tool, hardness, correctCategory, drops)
    }
    
    protected abstract fun createBreakMethod(clientsidePrediction: Boolean): BreakMethod
    protected abstract fun handleBreakTick()
    
}