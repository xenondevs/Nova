package xyz.xenondevs.nova.registry

import net.minecraft.resources.RegistryOps
import org.bukkit.block.BlockType
import xyz.xenondevs.commons.provider.flatten
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.provider.orElseBy
import xyz.xenondevs.nova.config.CONFIGS
import xyz.xenondevs.nova.world.block.NovaBlock

internal open class NovaBlockBuilderImpl(
    entry: RegistryEntry.Paper<BlockType>
) : AbstractNovaBlockBuilder<NovaBlock>(entry) {
    
    override fun build(lookup: RegistryOps.RegistryInfoLookup): NovaBlock =
        ScopedValue
            .where(NovaBlock.STATE_PROPERTIES, effectiveStateProperties)
            .call<NovaBlock, Nothing> {
                NovaBlock(
                    entry,
                    _name,
                    _style,
                    _behaviors,
                    effectiveStateProperties,
                    _item.orElseBy(NovaItemBuilderImpl.blockItems.getOrPut(entry) { mutableProvider { null } }),
                    _configId.map(CONFIGS::get),
                    _properties,
                    _flammable,
                    _selectFluidFlowMode,
                    _breakParticles.flatten(),
                    _showBreakAnimation,
                    _soundGroup
                )
            }
    
    
}
