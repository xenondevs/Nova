package xyz.xenondevs.nova.data.resources.model.blockstate

import net.minecraft.world.level.block.state.BlockState
import org.bukkit.Material
import xyz.xenondevs.nova.data.config.PermanentStorage
import kotlin.reflect.jvm.jvmName

sealed interface BlockStateConfig {
    val type: BlockStateConfigType<*>
    val id: Int
    val variantString: String
    val blockState: BlockState
}

abstract class BlockStateConfigType<T : BlockStateConfig> internal constructor() {
    
    abstract val maxId: Int
    abstract val blockedIds: Set<Int>
    abstract val fileName: String
    abstract val material: Material
    
    abstract fun of(id: Int): T
    abstract fun of(variantString: String): T
    internal open fun handleMerged(occupiedIds: Set<Int>) = Unit
    
}

abstract class DefaultingBlockStateConfigType<T : BlockStateConfig> internal constructor() : BlockStateConfigType<T>() {
    abstract val defaultStateConfig: T
}

abstract class DynamicDefaultingBlockStateConfigType<T : BlockStateConfig> internal constructor() : DefaultingBlockStateConfigType<T>() {
    
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