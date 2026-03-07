package xyz.xenondevs.nova.world.item.legacy

import net.minecraft.core.component.DataComponentPatch

internal sealed interface ItemStackLegacyConverter {
    
    val affectedItemIds: Set<String>?
        get() = null
    
    fun convert(patch: DataComponentPatch): DataComponentPatch
    
}