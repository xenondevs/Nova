package xyz.xenondevs.nova.world.block.state.model

import io.papermc.paper.datacomponent.DataComponentTypes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Fluid
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Display.Brightness
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import org.joml.Matrix4f
import org.joml.Matrix4fc
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.nova.initialize.DisableFun
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.packetentity.ItemDisplayMetadata
import xyz.xenondevs.nova.packetentity.PacketItemDisplay
import xyz.xenondevs.nova.packetentity.packetItemDisplay
import xyz.xenondevs.nova.packetentity.transform
import xyz.xenondevs.nova.serialization.kotlinx.DisplayEntityBlockModelDataSerializer
import xyz.xenondevs.nova.serialization.kotlinx.KeySerializer
import xyz.xenondevs.nova.serialization.kotlinx.Matrix4fcAsArraySerializer
import xyz.xenondevs.nova.util.item.requiresLight
import xyz.xenondevs.nova.util.levelChunk
import xyz.xenondevs.nova.util.nmsBlockState
import xyz.xenondevs.nova.util.nmsPos
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.chunkPos
import xyz.xenondevs.nova.world.item.DefaultBlockOverlays
import xyz.xenondevs.nova.world.pos
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Serializable(DisplayEntityBlockModelDataSerializer::class)
internal class DisplayEntityBlockModelData(
    val waterlogged: Boolean,
    val models: List<Model>,
    val colliderProvider: Provider<BlockData>
) {
    
    val collider: BlockData by colliderProvider
    
    @Serializable
    internal data class Model(
        @Serializable(with = KeySerializer::class)
        val model: Key,
        @Serializable(with = Matrix4fcAsArraySerializer::class)
        val transform: Matrix4fc
    ) {
        
        val itemStack: ItemStack
            get() = ItemType.PAPER.createItemStack().apply {
                @Suppress("UnstableApiUsage")
                setData(DataComponentTypes.ITEM_MODEL, model)
            }
        
    }
    
}

/**
 * A block model provider that uses display entities to display the block model.
 */
@Serializable
@SerialName("entity_backed")
internal class DisplayEntityBlockModelProvider(val info: DisplayEntityBlockModelData) : BlockModelProvider {
    
    @Suppress("unused")
    @InternalInit(stage = InternalInitStage.POST_WORLD)
    companion object : Listener {
        
        val entities = ConcurrentHashMap<Block, List<PacketItemDisplay>>()
        private val blocksByChunk = HashMap<ChunkPos, MutableSet<Block>>()
        
        @InitFun
        private fun init() {
            registerEvents()
            Bukkit.getWorlds()
                .asSequence()
                .flatMap { it.loadedChunks.asSequence() }
                .forEach(::loadChunk)
        }
        
        @DisableFun
        private fun disable() {
            entities.values.flatten().forEach(PacketItemDisplay::despawn)
            entities.clear()
            blocksByChunk.clear()
        }
        
        @EventHandler
        private fun handleChunkLoad(event: ChunkLoadEvent) {
            loadChunk(event.chunk)
        }
        
        @EventHandler
        private fun handleChunkUnload(event: ChunkUnloadEvent) {
            blocksByChunk.remove(event.chunk.pos)
                ?.forEach { block -> entities.remove(block)?.forEach(PacketItemDisplay::despawn) }
        }
        
        private fun loadChunk(chunk: Chunk) {
            val levelChunk = chunk.levelChunk
            val providerMaps = IdentityHashMap<NovaBlock, Map<BlockState, BlockModelProvider>>()
            
            fun getProvider(state: BlockState): DisplayEntityBlockModelProvider? {
                val block = state.block as? NovaBlock
                    ?: return null
                val providers = providerMaps.getOrPut(block) { block.modelProviders.get() }
                return providers[state] as? DisplayEntityBlockModelProvider
            }
            
            val blockX = chunk.x shl 4
            val blockZ = chunk.z shl 4
            for (sectionIndex in levelChunk.sections.indices) {
                val chunkSection = levelChunk.sections[sectionIndex]
                if (!chunkSection.maybeHas { getProvider(it) != null })
                    continue
                
                val blockY = levelChunk.getSectionYFromSectionIndex(sectionIndex) shl 4
                for (y in 0..<16) for (z in 0..<16) for (x in 0..<16) {
                    val provider = getProvider(chunkSection.getBlockState(x, y, z))
                        ?: continue
                    provider.load(chunk.world.getBlockAt(blockX + x, blockY + y, blockZ + z))
                }
            }
        }
        
        private fun track(block: Block) {
            blocksByChunk.getOrPut(block.chunkPos) { HashSet() }.add(block)
        }
        
        private fun untrack(block: Block) {
            val chunkPos = block.chunkPos
            val blocks = blocksByChunk[chunkPos] ?: return
            blocks.remove(block)
            if (blocks.isEmpty())
                blocksByChunk.remove(chunkPos)
        }
        
    }
    
    override val clientsideBlockState: BlockState
        get() = info.collider.nmsBlockState
    
    override fun load(block: Block) {
        if (entities.containsKey(block)) {
            replace(block, this)
            return
        }
        
        val models = info.models.mapTo(ArrayList()) { createDisplay(block, it) }
        if (info.waterlogged)
            models += createWaterlogDisplay(block)
        
        entities[block] = models
        track(block)
    }
    
    override fun unload(block: Block) {
        entities.remove(block)?.forEach(PacketItemDisplay::despawn)
        untrack(block)
    }
    
    override fun replace(block: Block, previous: BlockModelProvider) {
        if (previous !is DisplayEntityBlockModelProvider) {
            super.replace(block, previous)
            return
        }
        
        // re-use as many existing entities as possible
        val prevEntities = entities[block] ?: emptyList()
        val newEntities = ArrayList<PacketItemDisplay>()
        
        var i = 0
        for (model in info.models) {
            newEntities += prevEntities.getOrNull(i++)
                ?.also { prevEntity -> setMetadata(prevEntity.metadata, model) }
                ?: createDisplay(block, model)
        }
        if (info.waterlogged) {
            newEntities += prevEntities.getOrNull(i++)
                ?.also { prevEntity -> setWaterlogMetadata(prevEntity.metadata, block) }
                ?: createWaterlogDisplay(block)
        }
        
        for (j in i..<prevEntities.size)
            prevEntities[j].despawn()
        
        entities[block] = newEntities
        track(block)
    }
    
    fun updateWaterlogEntity(block: Block) {
        entities[block]?.lastOrNull()?.metadata?.let { setWaterlogMetadata(it, block) }
    }
    
    private fun createDisplay(block: Block, model: DisplayEntityBlockModelData.Model) = packetItemDisplay {
        location by block.location.toCenterLocation()
    }.apply {
        setMetadata(metadata, model)
        spawn()
    }
    
    private fun createWaterlogDisplay(pos: Block) = packetItemDisplay {
        location by pos.location.toCenterLocation()
    }.apply {
        setWaterlogMetadata(metadata, pos)
        spawn()
    }
    
    private fun setWaterlogMetadata(data: ItemDisplayMetadata, pos: Block) {
        data.brightnessOverride = null
        @Suppress("DEPRECATION")
        data.itemStack = ItemBuilder(DefaultBlockOverlays.WATERLOGGED.get())
            .setCustomModelData(0, pos.world.getFluidData(pos.x, pos.y + 1, pos.z).fluidType == Fluid.WATER)
            .setCustomModelData(0, Color(pos.world.serverLevel.getBiome(pos.nmsPos).value().waterColor))
            .build()
        data.transform = Matrix4f()
    }
    
    private fun setMetadata(data: ItemDisplayMetadata, model: DisplayEntityBlockModelData.Model) {
        // TODO: proper light level
        if (info.collider.material.requiresLight) {
            data.brightnessOverride = Brightness(15, 15)
        }
        
        data.itemStack = model.itemStack
        data.transform = model.transform
    }
    
}
