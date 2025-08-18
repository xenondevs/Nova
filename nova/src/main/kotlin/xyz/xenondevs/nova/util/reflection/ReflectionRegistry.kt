package xyz.xenondevs.nova.util.reflection

import net.minecraft.core.Holder
import net.minecraft.world.level.block.Block
import org.bukkit.event.inventory.PrepareItemCraftEvent
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getConstructor
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getField
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getMethod
import java.util.*

// TODO: Retire ReflectionRegistry, put the fields as top level constants in the files that use them instead
@Suppress("MemberVisibilityCanBePrivate")
internal object ReflectionRegistry {
    
    // Constructors
    val ENUM_MAP_CONSTRUCTOR = getConstructor(EnumMap::class, false, Class::class)
    
    // Methods
    val HOLDER_REFERENCE_BIND_VALUE_METHOD = getMethod(Holder.Reference::class, true, "bindValue", Any::class)
    
    // Fields
    val PREPARE_ITEM_CRAFT_EVENT_MATRIX_FIELD = getField(PrepareItemCraftEvent::class, true, "matrix")
    val BLOCK_DEFAULT_BLOCK_STATE_FIELD = getField(Block::class, true, "defaultBlockState")
    
}
