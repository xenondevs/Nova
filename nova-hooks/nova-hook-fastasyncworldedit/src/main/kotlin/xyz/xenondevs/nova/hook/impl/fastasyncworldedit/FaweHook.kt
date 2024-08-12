package xyz.xenondevs.nova.hook.impl.fastasyncworldedit

import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.event.extent.EditSessionEvent
import com.sk89q.worldedit.extension.input.ParserContext
import com.sk89q.worldedit.extent.AbstractDelegateExtent
import com.sk89q.worldedit.internal.registry.InputParser
import com.sk89q.worldedit.util.eventbus.Subscribe
import com.sk89q.worldedit.world.block.BaseBlock
import com.sk89q.worldedit.world.block.BlockStateHolder
import com.sk89q.worldedit.world.block.BlockTypes
import xyz.xenondevs.nova.integration.Hook
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.contains
import xyz.xenondevs.nova.util.get
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.format.WorldDataManager
import java.util.stream.Stream

private val BLOCK_STATE = BlockTypes.BARRIER!!.defaultState

@Hook(plugins = ["WorldEdit", "FastAsyncWorldEdit"], requireAll = true)
internal object FaweHook {
    
    init {
        val worldEdit = WorldEdit.getInstance()
        worldEdit.blockFactory.register(NovaBlockInputParser(worldEdit))
        worldEdit.eventBus.register(this)
    }
    
    @Subscribe
    fun handleEditSession(event: EditSessionEvent) {
        if (event.stage == EditSession.Stage.BEFORE_CHANGE) {
            event.extent = NovaBlockExtent(event)
        }
    }
    
}

class NovaBlock(val novaId: String) : BaseBlock(BLOCK_STATE)

internal class NovaBlockInputParser(worldEdit: WorldEdit) : InputParser<BaseBlock>(worldEdit) {
    
    override fun getSuggestions(input: String): Stream<String> {
        return NovaRegistries.BLOCK.stream()
            .filter { it.id.toString().startsWith(input) || it.id.path.startsWith(input) }
            .map { it.id.toString() }
    }
    
    override fun parseFromInput(input: String, context: ParserContext): BaseBlock? {
        if (input !in NovaRegistries.BLOCK)
            return null
        
        return NovaBlock(input)
    }
    
}

internal class NovaBlockExtent(private val event: EditSessionEvent) : AbstractDelegateExtent(event.extent) {
    
    override fun <T : BlockStateHolder<T>?> setBlock(x: Int, y: Int, z: Int, block: T): Boolean {
        if (block is NovaBlock) {
            val pos = BlockPos(BukkitAdapter.adapt(event.world), x, y, z)
            WorldDataManager.setBlockState(pos, NovaRegistries.BLOCK[block.novaId]!!.defaultBlockState)
            return true
        }
        
        return super.setBlock(x, y, z, block)
    }
    
}
