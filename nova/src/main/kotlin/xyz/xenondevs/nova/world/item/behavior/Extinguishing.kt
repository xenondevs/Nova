package xyz.xenondevs.nova.world.item.behavior

import net.minecraft.core.particles.ParticleType
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CampfireBlock
import net.minecraft.world.level.gameevent.GameEvent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.util.nmsBlockState
import xyz.xenondevs.nova.util.nmsEntity
import xyz.xenondevs.nova.util.particle.particle
import xyz.xenondevs.nova.util.sendTo
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.item.ItemAction
import kotlin.random.Random

private const val EXTINGUISH_CAMPFIRE_LEVEL_EVENT = 1009

/**
 * Allows items to extinguish campfires.
 */
object Extinguishing : ItemBehavior {
    
    override fun useOnBlock(itemStack: ItemStack, block: Block, ctx: Context<BlockInteract>): InteractionResult {
        val pos = ctx[BlockInteract.BLOCK_POS]
        val entity = ctx[BlockInteract.SOURCE_LIVING_ENTITY] ?: return InteractionResult.Pass
        val state = ctx[BlockInteract.BLOCK_STATE_VANILLA]?.nmsBlockState ?: return InteractionResult.Pass
        
        if (state.block != Blocks.CAMPFIRE || !state.getValue(CampfireBlock.LIT))
            return InteractionResult.Pass
        
        val nmsEntity = entity.nmsEntity
        val level = pos.world.serverLevel
        val nmsPos = pos.nmsPos
        
        CampfireBlock.dowse(nmsEntity, level, nmsPos, state)
        displayCampfireExtinguishParticles(
            pos.location,
            if (state.getValue(CampfireBlock.SIGNAL_FIRE)) ParticleTypes.CAMPFIRE_SIGNAL_SMOKE else ParticleTypes.CAMPFIRE_COSY_SMOKE
        )
        
        val newState = state.setValue(CampfireBlock.LIT, false)
        level.setBlock(nmsPos, newState, 11)
        level.levelEvent(null, EXTINGUISH_CAMPFIRE_LEVEL_EVENT, nmsPos, 0)
        level.gameEvent(GameEvent.BLOCK_CHANGE, nmsPos, GameEvent.Context.of(nmsEntity, newState))
        
        return InteractionResult.Success(swing = true, action = ItemAction.Damage())
    }
    
    private fun displayCampfireExtinguishParticles(loc: Location, type: ParticleType<*>) {
        val players = Bukkit.getOnlinePlayers().filter { it.location.world == loc.world && it.location.distance(loc) <= 100 }
        repeat(20) {
            particle(type) {
                location(loc.clone().add(
                    0.5 + Random.nextDouble(-0.33, 0.33),
                    0.5 + Random.nextDouble(-0.33, 0.33),
                    0.5 + Random.nextDouble(-0.33, 0.33),
                ))
                offsetY(0.07f)
            }.sendTo(players)
            
            particle(ParticleTypes.SMOKE) {
                location(loc.clone().add(
                    0.5 + Random.nextDouble(-0.25, 0.25),
                    0.5 + Random.nextDouble(-0.25, 0.25),
                    0.5 + Random.nextDouble(-0.25, 0.25),
                ))
                offsetY(0.005f)
            }.sendTo(players)
        }
    }
    
}