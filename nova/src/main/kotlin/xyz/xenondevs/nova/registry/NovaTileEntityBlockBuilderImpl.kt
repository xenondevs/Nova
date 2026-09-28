package xyz.xenondevs.nova.registry

import net.minecraft.core.WritableRegistry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.RegistryOps
import net.minecraft.world.level.block.entity.BlockEntityType
import org.bukkit.block.BlockType
import xyz.xenondevs.commons.provider.flatten
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.provider.orElseBy
import xyz.xenondevs.commons.provider.uninitializedProvider
import xyz.xenondevs.nova.config.CONFIGS
import xyz.xenondevs.nova.registry.KnownRegistryEntries.BlockConfiguration
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.NovaTileEntityProxy
import xyz.xenondevs.nova.world.block.TileEntityConstructor

internal class NovaTileEntityBlockBuilderImpl(
    entry: RegistryEntry.Paper<BlockType>,
    private val tileEntity: TileEntityConstructor
) : AbstractNovaBlockBuilder<NovaTileEntityBlock>(entry), NovaTileEntityBlockBuilder {
    
    private val _tickrate = uninitializedProvider<Int>()
    
    private var tickrate: Int by _tickrate
    
    override fun reset() {
        super.reset()
        tickrate = 20
    }
    
    override fun tickrate(tickrate: Int) {
        require(tickrate in 0..20) { "Sync TPS must be between 0 and 20" }
        this.tickrate = tickrate
    }
    
    override fun build(lookup: RegistryOps.RegistryInfoLookup): NovaTileEntityBlock {
        KnownRegistryEntries.knownBlockStates[entry.key] = BlockConfiguration(
            isTileEntity = true,
            properties = effectiveStateProperties.map { BlockConfiguration.Property(it.key, it.name, it.stringValues) }
        )
        
        val block = ScopedValue
            .where(NovaBlock.STATE_PROPERTIES, effectiveStateProperties) // cursed hack to pass state properties
            .call<NovaTileEntityBlock, Nothing> {
                NovaTileEntityBlock(
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
                    _hitParticles.flatten(),
                    _breakParticles.flatten(),
                    _showBreakAnimation,
                    tileEntity,
                    _tickrate
                )
            }
        
        // TODO: it would be cleaner if this were also orchestrated by RegistryLoader
        val blockEntityType = BlockEntityType({ pos, state -> NovaTileEntityProxy(block, pos, state) }, setOf(block))
        block.blockEntityType = blockEntityType
        (BuiltInRegistries.BLOCK_ENTITY_TYPE as WritableRegistry<BlockEntityType<*>>)[key] = blockEntityType
        
        return block
    }
    
}