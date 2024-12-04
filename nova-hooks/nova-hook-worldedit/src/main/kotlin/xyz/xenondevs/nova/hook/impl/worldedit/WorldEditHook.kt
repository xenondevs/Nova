package xyz.xenondevs.nova.hook.impl.worldedit

import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.event.extent.EditSessionEvent
import com.sk89q.worldedit.extension.input.ParserContext
import com.sk89q.worldedit.extent.AbstractDelegateExtent
import com.sk89q.worldedit.internal.registry.InputParser
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.util.eventbus.Subscribe
import com.sk89q.worldedit.world.block.BaseBlock
import com.sk89q.worldedit.world.block.BlockStateHolder
import com.sk89q.worldedit.world.block.BlockTypes
import xyz.xenondevs.nova.integration.Hook
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.contains
import xyz.xenondevs.nova.util.getValueOrThrow
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.format.WorldDataManager
import java.util.stream.Stream

private val BLOCK_STATE = BlockTypes.BARRIER!!.defaultState

@Hook(plugins = ["WorldEdit"], unless = ["FastAsyncWorldEdit"])
internal object WorldEditHook {
    
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
            .filter { it.id.toString().startsWith(input) || it.id.value().startsWith(input) }
            .map { it.id.toString() }
    }
    
    override fun parseFromInput(input: String, context: ParserContext): BaseBlock? {
        if (input !in NovaRegistries.BLOCK)
            return null
        
        return NovaBlock(input)
    }
    
}

internal class NovaBlockExtent(private val event: EditSessionEvent) : AbstractDelegateExtent(event.extent) {
    
    override fun <T : BlockStateHolder<T>?> setBlock(vec: BlockVector3, block: T): Boolean {
        if (block is NovaBlock) {
            val pos = BlockPos(BukkitAdapter.adapt(event.world), vec.x, vec.y, vec.z)
            WorldDataManager.setBlockState(pos, NovaRegistries.BLOCK.getValueOrThrow(block.novaId).defaultBlockState)
            return true
        }
        
        return super.setBlock(vec, block)
    }
    
}