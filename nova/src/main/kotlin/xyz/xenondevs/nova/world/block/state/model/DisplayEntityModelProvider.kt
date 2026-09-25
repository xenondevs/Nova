package xyz.xenondevs.nova.world.block.state.model

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import io.papermc.paper.datacomponent.DataComponentTypes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Display.Brightness
import org.bukkit.entity.Entity
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import org.joml.Intersectiond
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector2d
import xyz.xenondevs.commons.collections.mapToIntArray
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.network.packet.ClientboundSetPassengersPacket
import xyz.xenondevs.nova.network.packetHandler
import xyz.xenondevs.nova.network.send
import xyz.xenondevs.nova.packetentity.ItemDisplayMetadata
import xyz.xenondevs.nova.packetentity.PacketBlockDisplay
import xyz.xenondevs.nova.packetentity.PacketEntityPassengersDsl
import xyz.xenondevs.nova.packetentity.PacketEntityVisibility
import xyz.xenondevs.nova.packetentity.PacketItemDisplay
import xyz.xenondevs.nova.packetentity.isGlowing
import xyz.xenondevs.nova.packetentity.isInvisible
import xyz.xenondevs.nova.packetentity.packetBlockDisplay
import xyz.xenondevs.nova.packetentity.packetItemDisplay
import xyz.xenondevs.nova.packetentity.transform
import xyz.xenondevs.nova.serialization.kotlinx.DisplayEntityBlockModelDataSerializer
import xyz.xenondevs.nova.serialization.kotlinx.KeySerializer
import xyz.xenondevs.nova.serialization.kotlinx.Matrix4fcAsArraySerializer
import xyz.xenondevs.nova.util.BlockFaceUtils
import xyz.xenondevs.nova.util.item.requiresLight
import xyz.xenondevs.nova.util.levelChunk
import xyz.xenondevs.nova.util.nmsBlockState
import xyz.xenondevs.nova.util.nmsDirection
import xyz.xenondevs.nova.util.nmsEntity
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.block.ColliderCube
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.chunkPos
import xyz.xenondevs.nova.world.pos

@Serializable(DisplayEntityBlockModelDataSerializer::class)
internal class DisplayEntityBlockModelData(
    val waterlogged: Boolean,
    val models: List<Model>,
    val colliderProvider: Provider<BlockData>,
    val extraColliders: List<ColliderCube>
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
    
    override val clientsideBlockState: BlockState
        get() = info.collider.nmsBlockState.let { state ->
            if (state.hasProperty(BlockStateProperties.WATERLOGGED))
                state.setValue(BlockStateProperties.WATERLOGGED, info.waterlogged)
            else
                state
        }
    
    override fun load(block: Block) {
        if (DisplayEntityModelProviderManager.hasEntities(block)) {
            replace(block, this)
            return
        }
        
        val models = info.models.mapTo(ArrayList()) { createDisplay(block, it) }
        
        DisplayEntityModelProviderManager.setEntities(block, models, createColliderEntities(block))
    }
    
    override fun unload(block: Block) {
        DisplayEntityModelProviderManager.remove(block)
    }
    
    override fun replace(block: Block, previous: BlockModelProvider) {
        if (previous !is DisplayEntityBlockModelProvider) {
            super.replace(block, previous)
            return
        }
        
        // re-use as many existing entities as possible
        val prevEntities = DisplayEntityModelProviderManager.getDisplayEntities(block).orEmpty()
        val newEntities = ArrayList<PacketItemDisplay>()
        
        var i = 0
        for (model in info.models) {
            newEntities += prevEntities.getOrNull(i++)
                ?.also { prevEntity -> setMetadata(prevEntity.metadata, model) }
                ?: createDisplay(block, model)
        }
        
        for (j in i..<prevEntities.size)
            prevEntities[j].despawn()
        
        val prevColliderEntities = DisplayEntityModelProviderManager.getColliderEntities(block).orEmpty()
        val newColliderEntities = if (previous.info.extraColliders == info.extraColliders) {
            prevColliderEntities
        } else {
            prevColliderEntities.forEach(PacketBlockDisplay::despawn)
            createColliderEntities(block)
        }
        
        DisplayEntityModelProviderManager.setEntities(block, newEntities, newColliderEntities)
    }
    
    fun createFallingModel(entity: FallingBlock): FallingBlockModel {
        val viewers = entity.trackedBy.mapTo(HashSet(), Player::getUniqueId)
        val displays = info.models.map { model ->
            packetItemDisplay {
                location by entity.passengerLocation
                sendMovementPackets = false
            }.apply {
                viewerWhitelist = viewers
                setMetadata(metadata, model)
                metadata.transform = Matrix4f()
                    .translation(0f, 0.5f - entity.height.toFloat(), 0f)
                    .mul(model.transform)
            }
        }
        
        displays.lastOrNull()?.spawnHandlers?.add { viewer ->
            val realPassengers = entity.passengers.mapToIntArray(Entity::getEntityId)
            val modelPassengers = displays.mapToIntArray(PacketItemDisplay::id)
            viewer.send(ClientboundSetPassengersPacket(entity.entityId, realPassengers + modelPassengers))
        }
        return FallingBlockModel(entity, displays)
    }
    
    private fun createDisplay(block: Block, model: DisplayEntityBlockModelData.Model) = packetItemDisplay {
        location by block.location.toCenterLocation()
    }.apply {
        setMetadata(metadata, model)
        spawn()
    }
    
    private fun createColliderEntities(block: Block): List<PacketBlockDisplay> =
        info.extraColliders.map { cube ->
            val colliderPosition = block.location.add(cube.centerX, cube.minY, cube.centerZ)
            packetBlockDisplay {
                location by colliderPosition
                visibility = PacketEntityVisibility.NEAR
                metadata {
                    isInvisible by true
                    viewRange by 0f
                }
                passengers {
                    colliderShulker(block, colliderPosition, cube)
                }
            }.apply { spawn() }
        }
    
    private fun PacketEntityPassengersDsl.colliderShulker(
        block: Block,
        colliderPosition: Location,
        cube: ColliderCube
    ) = shulker {
        attributes[Attribute.SCALE] by cube.size
        
        metadata {
            isGlowing by DisplayEntityModelProviderManager.colliderOutlinesEnabled
            isInvisible by true
        }
        
        onAttackAsync { event ->
            event.isCancelled = true
            
            val player = event.player
            val eye = player.eyeLocation
            val direction = eye.direction
            val distances = Vector2d()
            val hit = Intersectiond.intersectRayAab(
                eye.x, eye.y, eye.z,
                direction.x, direction.y, direction.z,
                block.x + cube.minX, block.y + cube.minY, block.z + cube.minZ,
                block.x + cube.maxX, block.y + cube.maxY, block.z + cube.maxZ,
                distances
            )
            if (!hit)
                return@onAttackAsync
            
            val distance = if (distances.x >= 0.0) distances.x else distances.y
            val hitDirection = BlockFaceUtils.determineBlockFace(
                eye.x + direction.x * distance - block.x - cube.centerX,
                eye.y + direction.y * distance - block.y - cube.centerY,
                eye.z + direction.z * distance - block.z - cube.centerZ
            ).nmsDirection
            
            val packet = ServerboundPlayerActionPacket(START_DESTROY_BLOCK, block.nmsPos, hitDirection, 0)
            player.packetHandler?.injectIncoming(packet)
        }
        
        onInteractAsync { event ->
            event.isCancelled = true
            // Only main hand packet is relevant. This starts the consistent use loop.
            if (event.hand != InteractionHand.MAIN_HAND)
                return@onInteractAsync
            
            val player = event.player
            val relativeHit = event.location
            val hitLocation = Vec3(
                colliderPosition.x + relativeHit.x(),
                colliderPosition.y + relativeHit.y(),
                colliderPosition.z + relativeHit.z()
            )
            val hitResult = BlockHitResult(
                hitLocation,
                BlockFaceUtils.determineBlockFace(
                    relativeHit.x(),
                    relativeHit.y() - cube.size / 2.0,
                    relativeHit.z()
                ).nmsDirection,
                block.nmsPos,
                false
            )
            val packet = ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hitResult, 0, System.currentTimeMillis())
            player.packetHandler?.injectIncoming(packet)
        }
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

@Suppress("unused")
@InternalInit(stage = InternalInitStage.POST_WORLD)
internal object DisplayEntityModelProviderManager : Listener {
    
    private class Entities(
        val displayEntities: List<PacketItemDisplay>,
        val colliderEntities: List<PacketBlockDisplay>
    ) {
        
        fun despawn() {
            displayEntities.forEach(PacketItemDisplay::despawn)
            colliderEntities.forEach(PacketBlockDisplay::despawn)
        }
        
    }
    
    private val entitiesByChunk = HashMap<ChunkPos, HashMap<Block, Entities>>()
    private val fallingBlocks = HashMap<FallingBlock, FallingBlockModel>()
    
    var colliderOutlinesEnabled = false
        set(value) {
            field = value
            entitiesByChunk.values.asSequence()
                .flatMap { it.values }
                .flatMap { it.colliderEntities }
                .flatMap { it.passengers }
                .forEach { it.metadata.isGlowing = value }
        }
    
    @InitFun
    private fun init() {
        registerEvents()
        Bukkit.getWorlds().forEach { world ->
            world.loadedChunks.forEach(::loadChunk)
            world.entities
                .asSequence()
                .filterIsInstance<FallingBlock>()
                .forEach(::loadFallingBlock)
        }
        runTaskTimer(0, 1) {
            fallingBlocks.values.removeIf { !it.update() }
        }
    }
    
    @EventHandler
    private fun handleChunkLoad(event: ChunkLoadEvent) {
        loadChunk(event.chunk)
    }
    
    @EventHandler
    private fun handleChunkUnload(event: ChunkUnloadEvent) {
        entitiesByChunk.remove(event.chunk.pos)
            ?.values
            ?.forEach(Entities::despawn)
    }
    
    @EventHandler
    private fun handleEntityAdd(event: EntityAddToWorldEvent) {
        val entity = event.entity as? FallingBlock
            ?: return
        loadFallingBlock(entity)
    }
    
    @EventHandler
    private fun handleEntityRemove(event: EntityRemoveFromWorldEvent) {
        val entity = event.entity as? FallingBlock
            ?: return
        unloadFallingBlock(entity)
    }
    
    private fun loadChunk(chunk: Chunk) {
        val levelChunk = chunk.levelChunk
        val providerMaps = HashMap<NovaBlock, Map<BlockState, BlockModelProvider>>()
        
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
    
    fun hasEntities(block: Block): Boolean =
        entitiesByChunk[block.chunkPos]?.containsKey(block) == true
    
    fun getDisplayEntities(block: Block): List<PacketItemDisplay>? =
        entitiesByChunk[block.chunkPos]?.get(block)?.displayEntities
    
    fun getColliderEntities(block: Block): List<PacketBlockDisplay>? =
        entitiesByChunk[block.chunkPos]?.get(block)?.colliderEntities
    
    fun setEntities(
        block: Block,
        displayEntities: List<PacketItemDisplay>,
        colliderEntities: List<PacketBlockDisplay>
    ) {
        entitiesByChunk.getOrPut(block.chunkPos, ::HashMap)[block] = Entities(displayEntities, colliderEntities)
    }
    
    fun remove(block: Block) {
        val chunkPos = block.chunkPos
        val blocks = entitiesByChunk[chunkPos] ?: return
        blocks.remove(block)?.despawn()
        if (blocks.isEmpty())
            entitiesByChunk.remove(chunkPos)
    }
    
    private fun loadFallingBlock(entity: FallingBlock) {
        if (entity in fallingBlocks)
            return
        val modelProvider = entity.nmsEntity.blockState
            .let { (it.block as? NovaBlock)?.modelProviders?.get()[it] as? DisplayEntityBlockModelProvider }
            ?: return
        fallingBlocks[entity] = modelProvider.createFallingModel(entity)
    }
    
    private fun unloadFallingBlock(entity: FallingBlock) {
        fallingBlocks.remove(entity)?.detach()
    }
    
}

internal class FallingBlockModel(
    private val entity: FallingBlock,
    private val displays: List<PacketItemDisplay>
) {
    
    init {
        displays.forEach(PacketItemDisplay::spawn)
    }
    
    fun update(): Boolean {
        if (!entity.isValid) {
            detach()
            return false
        }
        
        val viewers = entity.trackedBy.mapTo(HashSet()) { it.uniqueId }
        displays.forEach { display ->
            display.location = entity.passengerLocation
            display.viewerWhitelist = viewers
        }
        
        return true
    }
    
    fun detach() {
        displays.forEach(PacketItemDisplay::despawn)
    }
}

private val FallingBlock.passengerLocation: Location
    get() = location.add(0.0, height, 0.0)
