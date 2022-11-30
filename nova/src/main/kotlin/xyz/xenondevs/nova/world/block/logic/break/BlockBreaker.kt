package xyz.xenondevs.nova.world.block.logic.`break`

import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.world.phys.Vec3
import org.bukkit.Axis
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.SoundCategory
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.craftbukkit.v1_19_R1.event.CraftEventFactory
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExpEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.ToolLevel
import xyz.xenondevs.nova.util.BlockFaceUtils
import xyz.xenondevs.nova.util.BlockUtils
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.axis
import xyz.xenondevs.nova.util.callEvent
import xyz.xenondevs.nova.util.getAllDrops
import xyz.xenondevs.nova.util.hardness
import xyz.xenondevs.nova.util.item.ToolUtils
import xyz.xenondevs.nova.util.item.damageToolBreakBlock
import xyz.xenondevs.nova.util.item.takeUnlessAir
import xyz.xenondevs.nova.util.nmsCopy
import xyz.xenondevs.nova.util.nmsPos
import xyz.xenondevs.nova.util.nmsState
import xyz.xenondevs.nova.util.particleBuilder
import xyz.xenondevs.nova.util.removeInternal
import xyz.xenondevs.nova.util.send
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.util.serverTick
import xyz.xenondevs.nova.util.soundGroup
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.block.sound.SoundGroup
import xyz.xenondevs.nova.world.pos
import xyz.xenondevs.particle.ParticleEffect
import kotlin.random.Random

private val MINING_FATIGUE = MobEffectInstance(MobEffect.byId(4), Integer.MAX_VALUE, 255, false, false, false)

internal class NovaBlockBreaker(
    player: Player,
    block: Block,
    val blockState: NovaBlockState,
    sequence: Int,
    blockedUntil: Int
) : BlockBreaker(player, block, sequence, blockedUntil) {
    
    val material = blockState.material
    override val breakMethod: BreakMethod? = BreakMethod.of(block, material)
    override val requiresToolForDrops: Boolean = material.requiresToolForDrops
    
    override fun handleBreakTick() {
        // spawn hit particles if not rendered clientside
        if (block.type == Material.BARRIER)
            spawnHitParticles()
    }
    
    private fun spawnHitParticles() {
        val texture = material.breakParticles ?: return
        val side = BlockFaceUtils.determineBlockFaceLookingAt(player.eyeLocation, 6.0, 0.2) ?: BlockFace.UP
        particleBuilder(ParticleEffect.ITEM_CRACK, block.location.add(0.5, 0.5, 0.5).advance(side, 0.6)) {
            Axis.values().forEach { if (it != side.axis) offset(it, 0.2f) }
            amount(1)
            speed(0f)
            texture(texture)
        }.display(player)
    }
    
}

internal class VanillaBlockBreaker(
    player: Player,
    block: Block,
    sequence: Int,
    blockedUntil: Int
) : BlockBreaker(player, block, sequence, blockedUntil) {
    
    override val breakMethod: BreakMethod = PacketBreakMethod(block.pos, Random.nextInt())
    override val requiresToolForDrops: Boolean = block.nmsState.requiresCorrectToolForDrops()
    
    override fun handleBreakTick() = Unit
    
}

@Suppress("MemberVisibilityCanBePrivate")
internal sealed class BlockBreaker(val player: Player, val block: Block, val sequence: Int, val blockedUntil: Int) {
    
    protected abstract val breakMethod: BreakMethod?
    protected abstract val requiresToolForDrops: Boolean
    
    protected val soundGroup: SoundGroup? = block.soundGroup
    protected val hardness: Double = block.hardness
    protected val tool: ItemStack? = player.inventory.itemInMainHand.takeUnlessAir()
    protected val toolCategory: ToolCategory? = ToolCategory.ofItem(tool)
    protected val correctCategory: Boolean = toolCategory != null && toolCategory.isCorrectToolCategoryForBlock(block)
    protected val correctLevel: Boolean = ToolLevel.isCorrectLevel(block, tool)
    protected val drops: Boolean by lazy { !requiresToolForDrops || (correctCategory && correctLevel) } // lazy because accessing abstract val
    
    private var destroyTicks = 0
    private var progress = 0.0
    private val isDone: Boolean
        get() = progress >= 1
    
    var isStopped: Boolean = false
        private set
    
    fun handleTick() {
        check(!isDone) { "Breaker is done" }
        
        val damage = calculateDamage()
        val clientsideDamage = calculateClientsideDamage()
        
        if (clientsideDamage >= 1 && damage < 1) {
            stop(true)
            return
        }
        
        if (damage >= 1.0 || serverTick >= blockedUntil) {
            progress += damage
            
            //<editor-fold desc="hit sounds", defaultstate="collapsed">
            if (progress < 1.0 && destroyTicks % 4 == 0) {
                if (soundGroup != null) {
                    block.pos.playSound(
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
        
        if (isDone) {
            // break block, call event, drop items and exp, etc.
            breakBlock(clientsideDamage >= 1) // If the block broke instantaneously for the client, the effects will also be played clientside
            // check if the breaker is still done (BlockBreakEvent cancelled?)
            if (isDone) {
                // Stop break animation and mining fatigue effect
                stop(false)
            }
        } else {
            // break tick logic of subclasses (i.e. spawning particles for barrier nova blocks)
            handleBreakTick()
            
            // set the break stage
            breakMethod?.let { it.breakStage = (progress * 10).toInt() - 1 }
            
            // re-send mining fatigue every tick to ensure that the player actually has it
            sendMiningFatigueEffect()
        }
    }
    
    private fun sendMiningFatigueEffect() {
        val effect = player.getPotionEffect(PotionEffectType.SLOW_DIGGING)
        val packet = if (effect != null) {
            // The player might actually have mining fatigue.
            // In this case, it is important to copy the hasIcon value to prevent it from disappearing.
            val effectInstance = MobEffectInstance(
                MobEffect.byId(4),
                Int.MAX_VALUE, 255,
                effect.isAmbient, effect.hasParticles(), effect.hasIcon()
            )
            ClientboundUpdateMobEffectPacket(player.entityId, effectInstance)
        } else {
            // The player does not have mining fatigue, we can use the default effect instance
            ClientboundUpdateMobEffectPacket(player.entityId, MINING_FATIGUE)
        }
        
        player.send(packet)
    }
    
    private fun breakBlock(brokenClientside: Boolean) {
        // create a block breaking context
        val ctx = BlockBreakContext(
            block.pos,
            player, player.location,
            BlockFaceUtils.determineBlockFaceLookingAt(player.eyeLocation, 8.0, 0.2),
            tool
        )
        
        val level = block.world.serverLevel
        val blockPos = block.pos.nmsPos
        
        //<editor-fold desc="break event", defaultstate="collapsed">
        val event = BlockBreakEvent(block, player)
        if (drops) {
            event.expToDrop = when (this) {
                is NovaBlockBreaker -> material.novaBlock.getExp(blockState, ctx)
                is VanillaBlockBreaker -> BlockUtils.getVanillaBlockExp(level, blockPos, tool.nmsCopy)
            }
        }
        callEvent(event)
        //</editor-fold>
        
        if (!event.isCancelled && !ProtectionManager.isVanillaProtected(player, block.location)) {
            //<editor-fold desc="item drops", defaultstate="collapsed">
            // TODO: block drops gamerule?
            // drop items
            if (event.isDropItems && (player.gameMode == GameMode.CREATIVE || drops)) {
                val itemEntities = block.getAllDrops(ctx).map {
                    ItemEntity(
                        level,
                        block.x + 0.5 + Random.nextDouble(-0.25, 0.25),
                        block.y + 0.5 + Random.nextDouble(-0.25, 0.25),
                        block.z + 0.5 + Random.nextDouble(-0.25, 0.25),
                        it.nmsCopy
                    ).apply(ItemEntity::setDefaultPickUpDelay)
                }
                CraftEventFactory.handleBlockDropItemEvent(block, block.state, player.serverPlayer, itemEntities)
            }
            //</editor-fold>
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
            
            // remove block
            block.removeInternal(ctx, true, !brokenClientside)
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
    
    fun stop(ack: Boolean) {
        isStopped = true
        breakMethod?.stop()
        
        val effect = player.getPotionEffect(PotionEffectType.SLOW_DIGGING)
        val effectPacket = if (effect != null) {
            // If the player actually has mining fatigue, send the correct effect again
            val effectInstance = MobEffectInstance(
                MobEffect.byId(4),
                effect.duration, effect.amplifier,
                effect.isAmbient, effect.hasParticles(), effect.hasIcon()
            )
            ClientboundUpdateMobEffectPacket(player.entityId, effectInstance)
        } else {
            // Remove the effect
            ClientboundRemoveMobEffectPacket(player.entityId, MobEffect.byId(4))
        }
        
        player.send(effectPacket)
        
        if (ack) {
            player.send(ClientboundBlockChangedAckPacket(sequence))
        }
    }
    
    private fun calculateClientsideDamage(): Double {
        return ToolUtils.calculateDamageVanilla(player, block)
    }
    
    private fun calculateDamage(): Double {
        return ToolUtils.calculateDamage(player, block, tool, hardness, correctCategory, drops)
    }
    
    protected abstract fun handleBreakTick()
    
}