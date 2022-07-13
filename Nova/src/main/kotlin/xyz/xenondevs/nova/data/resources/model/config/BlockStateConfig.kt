package xyz.xenondevs.nova.data.resources.model.config

import net.minecraft.world.level.block.state.BlockState

sealed interface BlockStateConfig {
    val type: BlockStateConfigType<*>
    val id: Int
    val variantString: String
    val blockState: BlockState
}

sealed interface BlockStateConfigType<T : BlockStateConfig> {
    
    val maxId: Int
    val blockedIds: Set<Int>
    val fileName: String
    
    fun of(id: Int): T
    
    fun of(variantString: String): T
    
    companion object {
        
        private val values = listOf(NoteBlockStateConfig)
        
        fun fromFileName(name: String): BlockStateConfigType<*>? =
            values.firstOrNull { it.fileName == name }
        
    }
    
}