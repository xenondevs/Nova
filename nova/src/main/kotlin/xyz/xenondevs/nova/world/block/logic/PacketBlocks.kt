package xyz.xenondevs.nova.world.block.logic

import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagNetworkSerialization
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.event.EventPriority
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.network.event.PacketHandler
import xyz.xenondevs.nova.network.event.PacketListener
import xyz.xenondevs.nova.network.event.clientbound.ClientboundAddEntityPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundLevelEventPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundUpdateTagsPacketEvent
import xyz.xenondevs.nova.network.event.registerPacketListener
import xyz.xenondevs.nova.resources.ResourceGeneration
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.util.REGISTRY_ACCESS
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.state.model.BackingStateBlockModelProvider
import java.util.IdentityHashMap

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    runAfter = [ResourceGeneration.PreWorld::class]
)
internal object PacketBlocks : PacketListener {
    
    private val clientSideBlockStateBitWidth: Int by lazy {
        val clientSideBlockStateCount = Block.BLOCK_STATE_REGISTRY.count {
            BuiltInRegistries.BLOCK.getKey(it.block).namespace == "minecraft"
        }
        Mth.ceillog2(clientSideBlockStateCount)
    }
    
    private val maskedBlockStates: Map<BlockState, BlockState>
        by ResourceLookups.blockModelLookup.map { lookup ->
            val map = IdentityHashMap<BlockState, BlockState>()
            for (value in lookup.values) {
                if (value is BackingStateBlockModelProvider)
                    map[value.info.vanillaBlockState] = value.info.maskedBlockState
            }
            map
        }
    
    @InitFun
    private fun init() {
        registerPacketListener()
    }
    
    @JvmStatic
    fun getClientSideState(state: BlockState): BlockState {
        val block = state.block
        return if (block is NovaBlock) {
            block.clientsideBlockStates[state] ?: state
        } else {
            maskedBlockStates[state] ?: state
        }
    }
    
    @JvmStatic
    fun getClientSideStateId(state: BlockState): Int {
        return Block.BLOCK_STATE_REGISTRY.tToId.getInt(getClientSideState(state))
    }
    
    @JvmStatic
    fun getClientSideBlockStateBits(): Int {
        return clientSideBlockStateBitWidth
    }
    
    @JvmStatic
    fun getClientSideBlock(block: Block): Block {
        return if (block is NovaBlock) block.clientsideBlock else block
    }
    
    @PacketHandler(priority = EventPriority.HIGHEST)
    private fun handleLevelEvent(event: ClientboundLevelEventPacketEvent) {
        if (event.type != 2001)
            return
        
        val state = Block.BLOCK_STATE_REGISTRY.byId(event.data)
            ?: return
        event.data = getClientSideStateId(state)
    }
    
    @PacketHandler(priority = EventPriority.HIGHEST)
    private fun handleAddEntity(event: ClientboundAddEntityPacketEvent) {
        if (event.type != EntityTypes.FALLING_BLOCK)
            return
        
        val state = Block.BLOCK_STATE_REGISTRY.byId(event.data)
            ?: return
        event.data = getClientSideStateId(state)
    }
    
    @PacketHandler(priority = EventPriority.HIGHEST)
    private fun handleBlockTags(event: ClientboundUpdateTagsPacketEvent) {
        event.tags = event.tags.mapValues { [key, payload] ->
            if (key != Registries.BLOCK)
                return@mapValues payload
            
            val registry = REGISTRY_ACCESS.lookupOrThrow(Registries.BLOCK)
            val tags = payload.resolve(registry).tags()
            val serialized = tags.entries.associate { [tagKey, tagValues] ->
                tagKey.location() to IntArrayList(tagValues.size).apply {
                    for (holder in tagValues) {
                        val block = holder.value()
                        if (block !is NovaBlock)
                            add(registry.getId(block))
                    }
                }
            }
            
            TagNetworkSerialization.NetworkPayload(serialized)
        }
    }
    
}
