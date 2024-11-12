package xyz.xenondevs.nova.world.item.behavior

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.HoneycombItem
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.world.level.block.WeatheringCopper
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.nmsEquipmentSlot
import xyz.xenondevs.nova.util.nmsInteractionHand
import xyz.xenondevs.nova.util.nmsState
import xyz.xenondevs.nova.util.runTaskLater
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.world.player.WrappedPlayerInteractEvent
import xyz.xenondevs.nova.world.pos
import net.minecraft.world.entity.player.Player as MojangPlayer

private const val REMOVE_WAX_LEVEL_EVENT = 3004
private const val SCRAPE_OXIDATION_LEVEL_EVENT = 3005

private val STRIPPABLES: Map<Block, Block> = mapOf(
    Blocks.OAK_WOOD to Blocks.STRIPPED_OAK_WOOD,
    Blocks.OAK_LOG to Blocks.STRIPPED_OAK_LOG,
    Blocks.DARK_OAK_WOOD to Blocks.STRIPPED_OAK_WOOD,
    Blocks.DARK_OAK_LOG to Blocks.STRIPPED_DARK_OAK_LOG,
    Blocks.ACACIA_WOOD to Blocks.STRIPPED_ACACIA_WOOD,
    Blocks.ACACIA_LOG to Blocks.STRIPPED_ACACIA_LOG,
    Blocks.BIRCH_WOOD to Blocks.STRIPPED_BIRCH_WOOD,
    Blocks.BIRCH_LOG to Blocks.STRIPPED_BIRCH_LOG,
    Blocks.JUNGLE_WOOD to Blocks.STRIPPED_JUNGLE_WOOD,
    Blocks.JUNGLE_LOG to Blocks.STRIPPED_JUNGLE_LOG,
    Blocks.SPRUCE_WOOD to Blocks.STRIPPED_SPRUCE_WOOD,
    Blocks.SPRUCE_LOG to Blocks.STRIPPED_SPRUCE_LOG,
    Blocks.WARPED_STEM to Blocks.STRIPPED_WARPED_STEM,
    Blocks.WARPED_HYPHAE to Blocks.STRIPPED_WARPED_HYPHAE,
    Blocks.CRIMSON_STEM to Blocks.STRIPPED_CRIMSON_STEM,
    Blocks.CRIMSON_HYPHAE to Blocks.STRIPPED_CRIMSON_HYPHAE,
    Blocks.MANGROVE_WOOD to Blocks.STRIPPED_MANGROVE_WOOD,
    Blocks.MANGROVE_LOG to Blocks.STRIPPED_MANGROVE_LOG,
    Blocks.CHERRY_LOG to Blocks.STRIPPED_CHERRY_LOG
)

/**
 * Allows items to strip blocks.
 */
object Stripping : ItemBehavior {
    
    override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, wrappedEvent: WrappedPlayerInteractEvent) {
        if (wrappedEvent.actionPerformed)
            return
        
        val event = wrappedEvent.event
        if (action == Action.RIGHT_CLICK_BLOCK) {
            val block = event.clickedBlock!!
            event.isCancelled = stripBlock(
                player.serverPlayer,
                event.hand!!.nmsInteractionHand,
                block.nmsState,
                block.world.serverLevel,
                block.pos.nmsPos
            )
        }
    }
    
    private fun stripBlock(player: MojangPlayer, hand: InteractionHand, state: BlockState, level: ServerLevel, pos: BlockPos): Boolean {
        val block = state.block
        val itemStack = player.getItemInHand(hand)
        
        fun setNewState(newState: BlockState) {
            runTaskLater(1) { player.swing(hand, true) }
            level.setBlock(pos, newState, 11)
            itemStack.hurtAndBreak(1, player, hand.nmsEquipmentSlot)
        }
        
        val stripped = STRIPPABLES[block]?.defaultBlockState()?.apply { setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS)) }
        if (stripped != null) {
            level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1f, 1f)
            setNewState(stripped)
            return true
        }
        
        val copper = WeatheringCopper.getPrevious(state).orElse(null)
        if (copper != null) {
            level.playSound(null, pos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1f, 1f)
            level.levelEvent(player, SCRAPE_OXIDATION_LEVEL_EVENT, pos, 0)
            setNewState(copper)
            return true
        }
        
        val honeycomb = HoneycombItem.WAX_OFF_BY_BLOCK.get()[block]?.withPropertiesOf(state)
        if (honeycomb != null) {
            level.playSound(null, pos, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1f, 1f)
            level.levelEvent(player, REMOVE_WAX_LEVEL_EVENT, pos, 0)
            setNewState(honeycomb)
            return true
        }
        
        return false
    }
    
}