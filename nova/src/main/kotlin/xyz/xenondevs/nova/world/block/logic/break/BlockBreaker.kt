package xyz.xenondevs.nova.world.block.logic.`break`

import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import org.bukkit.Axis
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.ToolLevel
import xyz.xenondevs.nova.util.BlockFaceUtils
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.axis
import xyz.xenondevs.nova.util.dropItems
import xyz.xenondevs.nova.util.getAllDrops
import xyz.xenondevs.nova.util.hardness
import xyz.xenondevs.nova.util.item.ToolUtils
import xyz.xenondevs.nova.util.item.damageToolBreakBlock
import xyz.xenondevs.nova.util.item.takeUnlessAir
import xyz.xenondevs.nova.util.nmsState
import xyz.xenondevs.nova.util.particleBuilder
import xyz.xenondevs.nova.util.remove
import xyz.xenondevs.nova.util.send
import xyz.xenondevs.nova.util.serverTick
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.pos
import xyz.xenondevs.particle.ParticleEffect
import kotlin.random.Random

private val MINING_FATIGUE = MobEffectInstance(MobEffect.byId(4), Integer.MAX_VALUE, 255, false, false, false)

internal class NovaBlockBreaker(
    player: Player,
    block: Block, blockState: NovaBlockState,
    sequence: Int,
    blockedUntil: Int
) : BlockBreaker(player, block, sequence, blockedUntil) {
    
    private val material = blockState.material
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

// TODO: break cooldown
@Suppress("MemberVisibilityCanBePrivate")
internal abstract class BlockBreaker(val player: Player, val block: Block, val sequence: Int, val blockedUntil: Int) {
    
    protected abstract val breakMethod: BreakMethod?
    protected abstract val requiresToolForDrops: Boolean
    
    protected val hardness: Double = block.hardness
    protected val tool: ItemStack? = player.inventory.itemInMainHand.takeUnlessAir()
    protected val toolCategory: ToolCategory? = ToolCategory.ofItem(tool)
    protected val correctCategory: Boolean = toolCategory != null && toolCategory.isCorrectToolCategoryForBlock(block)
    protected val correctLevel: Boolean = ToolLevel.isCorrectLevel(block, tool)
    protected val drops: Boolean by lazy { !requiresToolForDrops || (correctCategory && correctLevel) } // lazy because accessing abstract val
    
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
            stop()
            return
        }
        
        if (damage >= 1.0 || serverTick >= blockedUntil)
            progress += damage
        
        if (isDone) {
            // Stop break animation and mining fatigue effect
            stop()
            // create a block breaking context
            val ctx = BlockBreakContext(
                block.pos,
                player, player.location,
                BlockFaceUtils.determineBlockFaceLookingAt(player.eyeLocation, 8.0, 0.2),
                player.inventory.itemInMainHand.takeUnlessAir()
            )
            // Drop items
            if (player.gameMode == GameMode.CREATIVE || drops)
                block.location.dropItems(block.getAllDrops(ctx))
            // Damage tool
            if (player.gameMode != GameMode.CREATIVE && toolCategory != null && hardness > 0)
                player.damageToolBreakBlock()
            // If the block broke instantaneously for the client, the effects will also be played clientside
            val effects = clientsideDamage < 1
            block.remove(ctx, effects, effects)
            // The ack packet removes client-predicted block states and shows those sent by the server
            player.send(ClientboundBlockChangedAckPacket(sequence))
            // set cooldown for the next block
            BlockBreaking.setBreakCooldown(player)
        } else {
            // break tick logic of subclasses (i.e. spawning particles for barrier nova blocks)
            handleBreakTick()
            
            // set the break stage
            breakMethod?.let { it.breakStage = (progress * 10).toInt() - 1 }
            
            // give mining fatigue effect
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
    }
    
    fun stop() {
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
        
        val ackPacket = ClientboundBlockChangedAckPacket(sequence)
        player.send(effectPacket, ackPacket)
    }
    
    private fun calculateClientsideDamage(): Double {
        return ToolUtils.calculateDamageVanilla(player, block)
    }
    
    private fun calculateDamage(): Double {
        return ToolUtils.calculateDamage(player, block, tool, hardness, correctCategory, drops)
    }
    
    protected abstract fun handleBreakTick()
    
}