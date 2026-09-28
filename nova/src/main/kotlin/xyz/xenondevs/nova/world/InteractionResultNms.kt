package xyz.xenondevs.nova.world

import org.bukkit.craftbukkit.inventory.CraftItemStack
import xyz.xenondevs.nova.world.item.ItemAction
import net.minecraft.world.InteractionResult as NmsInteractionResult

internal fun NmsInteractionResult.toNova(): InteractionResult = when (this) {
    is NmsInteractionResult.Success -> {
        val swing = when (swingSource) {
            NmsInteractionResult.SwingSource.NONE,
            NmsInteractionResult.SwingSource.PREDICTED -> false
            NmsInteractionResult.SwingSource.SERVER_ONLY -> true
        }
        val action: ItemAction?
        if (itemContext.wasItemInteraction) {
            val transformedTo = itemContext.heldItemTransformedTo
            if (transformedTo != null) {
                action = ItemAction.ConvertStack(CraftItemStack.asBukkitCopy(transformedTo))
            } else {
                action = ItemAction.None
            }
        } else {
            action = null
        }
        InteractionResult.Success(swing, action)
    }
    
    is NmsInteractionResult.Fail -> InteractionResult.Fail
    is NmsInteractionResult.Pass -> InteractionResult.Pass
    is NmsInteractionResult.TryEmptyHandInteraction -> InteractionResult.Pass
}

internal fun InteractionResult.toNms(): NmsInteractionResult {
    return when (this) {
        is InteractionResult.Success -> NmsInteractionResult.Success(
            NmsInteractionResult.SwingSource.NONE,
            NmsInteractionResult.ItemContext(wasItemInteraction, null)
        )
        
        is InteractionResult.Fail -> NmsInteractionResult.FAIL
        is InteractionResult.Pass -> NmsInteractionResult.PASS
    }
}
