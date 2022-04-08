package xyz.xenondevs.nova.world.block

import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.*
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import org.bukkit.Axis
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.material.BlockNovaMaterial
import xyz.xenondevs.nova.material.CoreBlockOverlay
import xyz.xenondevs.nova.network.event.serverbound.PlayerActionPacketEvent
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.util.item.ToolCategory
import xyz.xenondevs.nova.util.item.ToolUtils
import xyz.xenondevs.nova.util.item.takeUnlessAir
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.pos
import xyz.xenondevs.particle.ParticleEffect
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import net.minecraft.world.entity.EquipmentSlot as MojangSlot

internal object BlockBreaking : Listener {
    
    private val playerBreakers = ConcurrentHashMap<Player, Breaker>()
    private val internalBreakers = HashMap<Int, BreakMethod>()
    
    fun init() {
        Bukkit.getPluginManager().registerEvents(this, NOVA)
        runTaskTimer(0, 1, ::handleTick)
    }
    
    internal fun setBreakStage(pos: BlockPos, entityId: Int, stage: Int) {
        val blockState = BlockManager.getBlock(pos) ?: return
        
        val block = pos.block
        var method = internalBreakers[entityId]
        
        // check that this is a valid stage, otherwise remove the current break effect
        if (stage !in 0..9) {
            method?.stop()
            internalBreakers -= entityId
            return
        }
        
        // check that the previous break effect is on that block, otherwise cancel the previous effect
        if (method != null && method.pos != blockState.pos) {
            method.stop()
            method = null
            internalBreakers -= entityId
        }
        
        // create a new break method if there isn't one
        if (method == null) {
            method = getBreakMethod(block, blockState.material, entityId) ?: return
            internalBreakers[entityId] = method
        }
        
        // set the break stage
        method.breakStage = stage
    }
    
    private fun handleTick() {
        playerBreakers.removeIf { (_, breaker) ->
            breaker.handleTick()
            return@removeIf breaker.isDone()
        }
    }
    
    private fun handleDestroyStart(player: Player, pos: BlockPos): Boolean {
        val blockState = BlockManager.getBlock(pos)
        if (blockState != null) {
            val material = blockState.material
            if (material.hardness >= 0) {
                playerBreakers[player] = Breaker(player, pos.location.block, blockState)
                return true
            }
        }
        
        return false
    }
    
    private fun handleDestroyStop(player: Player): Boolean {
        val breaker = playerBreakers.remove(player)
        if (breaker != null) {
            breaker.stop()
            return true
        }
        return false
    }
    
    @EventHandler
    private fun handlePlayerAction(event: PlayerActionPacketEvent) {
        val player = event.player
        val pos = event.pos
        val blockPos = BlockPos(event.player.world, pos.x, pos.y, pos.z)
        
        event.isCancelled = when (event.action) {
            START_DESTROY_BLOCK -> handleDestroyStart(player, blockPos)
            STOP_DESTROY_BLOCK, ABORT_DESTROY_BLOCK -> handleDestroyStop(player)
            else -> false
        }
    }
    
}

private fun getBreakMethod(block: Block, material: BlockNovaMaterial, entityId: Int = Random.nextInt()): BreakMethod? =
    if (material.showBreakAnimation)
        if (block.type == Material.BARRIER) ArmorStandBreakMethod(block.pos)
        else PacketBreakMethod(block.pos, entityId)
    else null

private val MINING_FATIGUE = MobEffectInstance(MobEffect.byId(4), Integer.MAX_VALUE, 255, false, false, false)

private class Breaker(val player: Player, val block: Block, val blockState: NovaBlockState) {
    
    private val material = blockState.material
    private val tool: ItemStack = player.inventory.itemInMainHand
    private val toolCategory: ToolCategory? = ToolCategory.of(tool.type)
    private val correctCategory: Boolean = toolCategory != null && material.toolCategory == toolCategory
    private val correctLevel: Boolean = material.toolLevel == null || tool.type in material.toolLevel.materialsWithHigherTier
    private val drops: Boolean = !material.requiresToolForDrops || (correctCategory && correctLevel)
    
    private val breakMethod: BreakMethod? = getBreakMethod(block, material)
    
    private var progress = 0.0
    
    fun isDone() = progress >= 1
    
    fun handleTick() {
        check(!isDone()) { "Breaker is done" }
        
        progress += calculateDamage()
        
        if (isDone()) {
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
            if (player.gameMode != GameMode.CREATIVE && drops)
                blockState.pos.location.dropItems(material.novaBlock.getDrops(blockState, ctx))
            // If the block broke instantaneously for the client, the effects will also be played clientside
            block.remove(ctx, calculateClientsideDamage() < 1)
        } else {
            // spawn hit particles if not rendered clientside
            if (block.type == Material.BARRIER) spawnHitParticles()
            
            // set the break stage
            val percentage = progress / 1
            if (breakMethod != null) breakMethod.breakStage = (percentage * 10).toInt() - 1
            
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
        breakMethod?.stop()
        
        val effect = player.getPotionEffect(PotionEffectType.SLOW_DIGGING)
        val packet = if (effect != null) {
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
        
        player.send(packet)
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
    
    private fun calculateDamage(): Double {
        if (player.gameMode == GameMode.CREATIVE) return 1.0
        return ToolUtils.calculateDamage(player, tool, toolCategory, material.hardness, correctCategory, drops)
    }
    
    private fun calculateClientsideDamage(): Double {
        if (player.gameMode == GameMode.CREATIVE) return 1.0
        return ToolUtils.calculateDamage(player, tool, toolCategory, block.type.hardness.toDouble(), correctCategory, drops)
    }
    
}

private abstract class BreakMethod(val pos: BlockPos) {
    
    val block = pos.block
    
    abstract var breakStage: Int
    
    abstract fun stop()
    
}

private class PacketBreakMethod(pos: BlockPos, val fakeEntityId: Int) : BreakMethod(pos) {
    
    override var breakStage: Int = -1
        set(stage) {
            if (field == stage) return
            
            field = stage
            block.sendDestructionPacket(fakeEntityId, stage)
        }
    
    override fun stop() {
        block.sendDestructionPacket(fakeEntityId, -1)
    }
    
}

private class ArmorStandBreakMethod(pos: BlockPos) : BreakMethod(pos) {
    
    private val armorStand = FakeArmorStand(pos.location.center(), true) {
        it.isInvisible = true
        it.isMarker = true
    }
    
    override var breakStage: Int = -1
        set(stage) {
            if (field == stage) return
            
            field = stage
            if (stage in 0..9) {
                armorStand.setEquipment(MojangSlot.HEAD, CoreBlockOverlay.BREAK_STAGE_OVERLAY.item.createItemStack(stage))
                armorStand.updateEquipment()
            } else {
                armorStand.setEquipment(MojangSlot.HEAD, null)
                armorStand.updateEquipment()
            }
        }
    
    override fun stop() {
        armorStand.remove()
    }
    
}