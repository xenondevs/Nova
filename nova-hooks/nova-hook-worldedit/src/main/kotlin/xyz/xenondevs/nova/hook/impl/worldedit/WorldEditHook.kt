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
import net.kyori.adventure.key.Key
import org.bukkit.Registry
import org.bukkit.block.BlockType
import xyz.xenondevs.nova.integration.Hook
import java.util.stream.Stream

// fixme: Only sets barries currently

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

class CustomBlock(val type: BlockType) : BaseBlock(BLOCK_STATE)

internal class NovaBlockInputParser(worldEdit: WorldEdit) : InputParser<BaseBlock>(worldEdit) {
    
    override fun getSuggestions(input: String): Stream<String> {
        return Registry.BLOCK.stream()
            .filter { it.key.asString().startsWith(input) || it.key.value().startsWith(input) }
            .map { it.key.asString() }
    }
    
    override fun parseFromInput(input: String, context: ParserContext): BaseBlock? {
        if (!Key.parseable(input))
            return null
        
        val key = Key.key(input)
        val type = Registry.BLOCK.get(key)
        if (key.namespace() == "minecraft" || type == null)
            return null
        
        return CustomBlock(type)
    }
    
}

internal class NovaBlockExtent(private val event: EditSessionEvent) : AbstractDelegateExtent(event.extent) {
    
    override fun <T : BlockStateHolder<T>?> setBlock(vec: BlockVector3, block: T): Boolean {
        if (block is CustomBlock) {
            BukkitAdapter.adapt(event.world).setBlockData(vec.x, vec.y, vec.z, block.type.createBlockData())
            return true
        }
        
        return super.setBlock(vec, block)
    }
    
}