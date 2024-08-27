package xyz.xenondevs.nova.world.block.state.model

import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.nova.config.PermanentStorage.storedValue

internal abstract class BackingStateConfig internal constructor() {
    
    abstract val type: BackingStateConfigType<*>
    abstract val id: Int
    abstract val waterlogged: Boolean
    abstract val variantString: String
    abstract val vanillaBlockState: BlockState
    
    override fun equals(other: Any?): Boolean {
        if (other !is BackingStateConfig)
            return false
        
        return type == other.type && id == other.id && waterlogged == other.waterlogged
    }
    
    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + id
        result = 31 * result + waterlogged.hashCode()
        return result
    }
    
}

internal abstract class BackingStateConfigType<T : BackingStateConfig> internal constructor(
    val maxId: Int,
    val fileName: String
) {
    
    abstract val blockedIds: Set<Int>
    
    abstract fun of(id: Int, waterlogged: Boolean = false): T
    abstract fun of(properties: Map<String, String>): T
    internal open fun handleMerged(occupiedIds: Set<Int>) = Unit
    
}

internal abstract class DefaultingBackingStateConfigType<T : BackingStateConfig>(
    maxId: Int,
    fileName: String
) : BackingStateConfigType<T>(maxId, fileName) {
    abstract val defaultStateConfig: T
}

internal abstract class DynamicDefaultingBackingStateConfigType<T : BackingStateConfig>(
    maxId: Int,
    fileName: String
) : DefaultingBackingStateConfigType<T>(maxId, fileName) {
    
    override val defaultStateConfig: T
        get() = of(defaultId)
    
    override val blockedIds: Set<Int>
        get() = hashSetOf(defaultId)
    
    private var defaultId: Int by storedValue("default_backing_state_config_id_$fileName") { 0 }
    
    override fun handleMerged(occupiedIds: Set<Int>) {
        defaultId = (0..maxId).first { it !in occupiedIds }
    }
    
}