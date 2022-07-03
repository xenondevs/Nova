package xyz.xenondevs.nova.data.resources.model.config

import xyz.xenondevs.nova.world.block.model.BlockModelProviderType

sealed interface BlockStateConfig {
    val id: Int
    val variantString: String
}

sealed interface BlockStateConfigType<T : BlockStateConfig> {
    
    val maxId: Int
    val blockedIds: Set<Int>
    val fileName: String
    val modelProvider: BlockModelProviderType<*>
    
    fun of(id: Int): T
    
    fun of(variantString: String): T
    
    companion object {
        
        private val values = listOf(NoteBlockStateConfig)
        
        fun fromFileName(name: String): BlockStateConfigType<*>? =
            values.firstOrNull { it.fileName == name }
        
    }
    
}