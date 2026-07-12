package xyz.xenondevs.nova.world.block.state.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.level.block.state.BlockState

/**
 * A block model provider that uses vanilla block states to display the block model.
 */
@Serializable
@SerialName("state_backed")
internal class BackingStateBlockModelProvider(val info: BackingStateConfig) : BlockModelProvider {
    override val clientsideBlockState: BlockState
        get() = info.vanillaBlockState
}