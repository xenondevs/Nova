package xyz.xenondevs.nova.world.block.behavior

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPhysicsEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.util.nmsState
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.util.setBlockStateSilently
import xyz.xenondevs.nova.world.block.behavior.impl.BrownMushroomBlockBehavior
import xyz.xenondevs.nova.world.block.behavior.impl.MushroomStemBlockBehavior
import xyz.xenondevs.nova.world.block.behavior.impl.RedMushroomBlockBehavior
import xyz.xenondevs.nova.world.block.behavior.impl.noteblock.NoteBlockBehavior
import xyz.xenondevs.nova.world.pos

internal object BlockBehaviorManager : Initializable(), Listener {
    
    override val inMainThread = true
    override val dependsOn = emptySet<Initializable>()
    
    private val behaviors: List<BlockBehavior> = listOf(
        NoteBlockBehavior,
        RedMushroomBlockBehavior,
        BrownMushroomBlockBehavior,
        MushroomStemBlockBehavior
    )
    
    override fun init() {
        LOGGER.info("Initializing block behaviors")
        registerEvents()
        behaviors.forEach(BlockBehavior::init)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    private fun handlePhysics(event: BlockPhysicsEvent) {
        val blockState = event.block.nmsState
        val behavior = behaviors.firstOrNull { blockState.block == it.defaultState.block } ?: return
        val pos = event.block.pos
        
        val task = {
            val correctState = behavior.getCorrectBlockState(pos)
            if (correctState != null) pos.setBlockStateSilently(correctState)
        }
        
        if (behavior.runUpdateLater)
            runTask(task)
        else task()
    }
    
}