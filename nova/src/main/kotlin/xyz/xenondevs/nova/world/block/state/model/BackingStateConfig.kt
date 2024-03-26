package xyz.xenondevs.nova.world.block.state.model

import net.minecraft.world.level.block.state.BlockState
import org.bukkit.Material
import xyz.xenondevs.nova.data.config.PermanentStorage
import kotlin.reflect.jvm.jvmName

internal abstract class BackingStateConfig internal constructor() {
    
    abstract val type: BackingStateConfigType<*>
    abstract val id: Int
    abstract val variantString: String
    abstract val vanillaBlockState: BlockState
    
    override fun equals(other: Any?): Boolean {
        if (other !is BackingStateConfig)
            return false
        
        return type == other.type && id == other.id
    }
    
    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + id
        return result
    }
    
}

internal abstract class BackingStateConfigType<T : BackingStateConfig> internal constructor() {
    
    abstract val maxId: Int
    abstract val blockedIds: Set<Int>
    abstract val fileName: String
    abstract val material: Material
    
    abstract fun of(id: Int): T
    abstract fun of(variantString: String): T
    internal open fun handleMerged(occupiedIds: Set<Int>) = Unit
    
    
}

internal abstract class DefaultingBackingStateConfigType<T : BackingStateConfig> internal constructor() : BackingStateConfigType<T>() {
    abstract val defaultStateConfig: T
}

internal abstract class DynamicDefaultingBackingStateConfigType<T : BackingStateConfig> internal constructor() : DefaultingBackingStateConfigType<T>() {
    
    override val defaultStateConfig: T
        get() = of(defaultId)
    
    override val blockedIds: Set<Int>
        get() = hashSetOf(defaultId)
    
    private var defaultId: Int = PermanentStorage.retrieve("defaultBlockStateConfig_${this::class.jvmName}") { 0 }
        private set(value) {
            field = value
            PermanentStorage.store("defaultBlockStateConfig_${this::class.jvmName}", value)
        }
    
    override fun handleMerged(occupiedIds: Set<Int>) {
        defaultId = (0..maxId).first { it !in occupiedIds }
    }
    
}