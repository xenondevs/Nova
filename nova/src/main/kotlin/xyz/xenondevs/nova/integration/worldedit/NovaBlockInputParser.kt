package xyz.xenondevs.nova.integration.worldedit

import com.sk89q.jnbt.CompoundTag
import com.sk89q.jnbt.StringTag
import com.sk89q.jnbt.Tag
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.extension.input.ParserContext
import com.sk89q.worldedit.internal.registry.InputParser
import com.sk89q.worldedit.world.block.BaseBlock
import com.sk89q.worldedit.world.block.BlockState
import com.sk89q.worldedit.world.block.BlockTypes
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.contains
import xyz.xenondevs.nova.util.name
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import java.util.stream.Stream

internal class NovaBlockInputParser(worldEdit: WorldEdit) : InputParser<BaseBlock>(worldEdit) {
    
    private val baseBlockConstructor = ReflectionUtils.getConstructor(BaseBlock::class.java, true, BlockState::class.java, CompoundTag::class.java)
    private val blockState = BlockTypes.BARRIER!!.defaultState
    
    override fun getSuggestions(input: String): Stream<String> {
        return NovaRegistries.BLOCK.stream()
            .filter { it.id.toString().startsWith(input) || it.id.name.startsWith(input) }
            .map { it.id.toString() }
    }
    
    override fun parseFromInput(input: String, context: ParserContext): BaseBlock? {
        if (input !in NovaRegistries.BLOCK)
            return null
        
        val compoundTag = CompoundTag(hashMapOf("nova" to StringTag(input)) as Map<String, Tag>)
        return baseBlockConstructor.newInstance(blockState, compoundTag)
    }
    
}