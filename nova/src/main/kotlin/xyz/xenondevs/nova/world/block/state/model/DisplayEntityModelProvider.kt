package xyz.xenondevs.nova.world.block.state.model

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.math.BlockPosition
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Display.Brightness
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import org.joml.Intersectiond
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector2d
import xyz.xenondevs.commons.collections.mapToIntArray
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.network.event.serverbound.ServerboundAttackPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundInteractPacketEvent
import xyz.xenondevs.nova.network.packet.ClientboundSetPassengersPacket
import xyz.xenondevs.nova.network.packetHandler
import xyz.xenondevs.nova.network.send
import xyz.xenondevs.nova.packetentity.ItemDisplayMetadata
import xyz.xenondevs.nova.packetentity.PacketBlockDisplay
import xyz.xenondevs.nova.packetentity.PacketEntityPassengersDsl
import xyz.xenondevs.nova.packetentity.PacketEntityVisibility
import xyz.xenondevs.nova.packetentity.PacketInteraction
import xyz.xenondevs.nova.packetentity.PacketItemDisplay
import xyz.xenondevs.nova.packetentity.isGlowing
import xyz.xenondevs.nova.packetentity.isInvisible
import xyz.xenondevs.nova.packetentity.packetBlockDisplay
import xyz.xenondevs.nova.packetentity.packetInteraction
import xyz.xenondevs.nova.packetentity.packetItemDisplay
import xyz.xenondevs.nova.packetentity.transform
import xyz.xenondevs.nova.serialization.kotlinx.DisplayEntityBlockModelDataSerializer
import xyz.xenondevs.nova.serialization.kotlinx.KeySerializer
import xyz.xenondevs.nova.serialization.kotlinx.Matrix4fcAsArraySerializer
import xyz.xenondevs.nova.util.BlockFaceUtils
import xyz.xenondevs.nova.util.item.requiresLight
import xyz.xenondevs.nova.util.nmsBlockState
import xyz.xenondevs.nova.util.nmsDirection
import xyz.xenondevs.nova.world.block.ColliderCube
import xyz.xenondevs.nova.world.block.HitboxCuboid
import java.util.*
import java.util.concurrent.atomic.AtomicReference

@Serializable(DisplayEntityBlockModelDataSerializer::class)
internal class DisplayEntityBlockModelData(
    val waterlogged: Boolean,
    val models: List<Model>,
    val colliderProvider: Provider<BlockData>,
    val extraColliders: List<ColliderCube>,
    val extraHitboxes: List<HitboxCuboid>
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
        DisplayEntityModelProviderManager.load(block, this)
    }
    
    override fun unload(block: Block) {
        DisplayEntityModelProviderManager.remove(block)
    }
    
    override fun replace(block: Block, previous: BlockModelProvider) {
        if (previous !is DisplayEntityBlockModelProvider) {
            super.replace(block, previous)
            return
        }
        DisplayEntityModelProviderManager.load(block, this)
    }
    
    internal fun createDisplayEntities(
        world: World,
        pos: BlockPosition,
        previous: List<PacketItemDisplay>
    ): List<PacketItemDisplay> {
        val newEntities = ArrayList<PacketItemDisplay>()
        
        var i = 0
        for (model in info.models) {
            newEntities += previous.getOrNull(i++)
                ?.also { prevEntity -> setMetadata(prevEntity.metadata, model) }
                ?: createDisplay(world, pos, model)
        }
        for (j in i..<previous.size)
            previous[j].despawn()
        
        return newEntities
    }
    
    internal fun createFallingDisplays(
        id: Int,
        passengerLocation: Location,
        height: Float,
        viewers: Set<UUID>,
        passengers: AtomicReference<IntArray>
    ): List<PacketItemDisplay> {
        val displays = info.models.map { model ->
            packetItemDisplay {
                location by passengerLocation
                sendMovementPackets = false
            }.apply {
                viewerWhitelist = viewers
                setMetadata(metadata, model)
                metadata.transform = Matrix4f()
                    .translation(0f, 0.5f - height, 0f)
                    .mul(model.transform)
            }
        }
        
        displays.lastOrNull()?.spawnHandlers?.add { viewer ->
            val modelPassengers = displays.mapToIntArray(PacketItemDisplay::id)
            viewer.send(ClientboundSetPassengersPacket(id, passengers.get() + modelPassengers))
        }
        return displays
    }
    
    private fun createDisplay(world: World, pos: BlockPosition, model: DisplayEntityBlockModelData.Model) = packetItemDisplay {
        location by Location(world, pos.blockX() + 0.5, pos.blockY() + 0.5, pos.blockZ() + 0.5)
    }.apply {
        setMetadata(metadata, model)
        spawn()
    }
    
    internal fun createColliderEntities(world: World, pos: BlockPosition): List<PacketBlockDisplay> =
        info.extraColliders.map { cube ->
            val colliderPosition = Location(world, pos.blockX() + cube.centerX, pos.blockY() + cube.minY, pos.blockZ() + cube.centerZ)
            packetBlockDisplay {
                location by colliderPosition
                visibility = PacketEntityVisibility.NEAR
                metadata {
                    isInvisible by true
                    viewRange by 0f
                }
                passengers {
                    colliderShulker(pos, colliderPosition, cube)
                }
            }.apply { spawn() }
        }
    
    private fun PacketEntityPassengersDsl.colliderShulker(
        pos: BlockPosition,
        colliderPosition: Location,
        cube: ColliderCube
    ) = shulker {
        val hitbox = HitboxCuboid.fromCollider(cube)
        attributes[Attribute.SCALE] by cube.size
        metadata {
            isGlowing by DisplayEntityModelProviderManager.colliderOutlinesEnabled
            isInvisible by true
        }
        onAttackAsync { event -> handleColliderAttack(event, pos, hitbox) }
        onInteractAsync { event -> handleColliderInteract(event, pos, colliderPosition, hitbox) }
    }
    
    internal fun createInteractionEntities(world: World, pos: BlockPosition): List<PacketInteraction> =
        info.extraHitboxes.map { hitbox ->
            val hitboxPosition = Location(world, pos.blockX() + hitbox.centerX, pos.blockY() + hitbox.minY, pos.blockZ() + hitbox.centerZ)
            packetInteraction {
                location by hitboxPosition
                metadata {
                    width by hitbox.width.toFloat()
                    height by hitbox.height.toFloat()
                }
                onAttackAsync { event -> handleColliderAttack(event, pos, hitbox) }
                onInteractAsync { event -> handleColliderInteract(event, pos, hitboxPosition, hitbox) }
            }.apply { spawn() }
        }
    
    private fun handleColliderAttack(event: ServerboundAttackPacketEvent, pos: BlockPosition, hitbox: HitboxCuboid) {
        event.isCancelled = true
        
        val x = pos.blockX()
        val y = pos.blockY()
        val z = pos.blockZ()
        val player = event.player
        val eye = player.eyeLocation
        val direction = eye.direction
        val distances = Vector2d()
        val hit = Intersectiond.intersectRayAab(
            eye.x, eye.y, eye.z,
            direction.x, direction.y, direction.z,
            x + hitbox.minX, y + hitbox.minY, z + hitbox.minZ,
            x + hitbox.maxX, y + hitbox.maxY, z + hitbox.maxZ,
            distances
        )
        if (!hit)
            return
        
        val distance = if (distances.x >= 0.0) distances.x else distances.y
        val hitDirection = determineHitboxFace(
            eye.x + direction.x * distance - x - hitbox.centerX,
            eye.y + direction.y * distance - y - hitbox.centerY,
            eye.z + direction.z * distance - z - hitbox.centerZ,
            hitbox
        )
        
        val packet = ServerboundPlayerActionPacket(START_DESTROY_BLOCK, BlockPos(x, y, z), hitDirection, 0)
        player.packetHandler?.injectIncoming(packet)
    }
    
    private fun handleColliderInteract(
        event: ServerboundInteractPacketEvent,
        pos: BlockPosition,
        entityPosition: Location,
        hitbox: HitboxCuboid
    ) {
        event.isCancelled = true
        // Only main hand packet is relevant. This starts the consistent use loop.
        if (event.hand != InteractionHand.MAIN_HAND)
            return
        
        val relativeHit = event.location
        val hitLocation = Vec3(
            entityPosition.x + relativeHit.x(),
            entityPosition.y + relativeHit.y(),
            entityPosition.z + relativeHit.z()
        )
        val hitResult = BlockHitResult(
            hitLocation,
            determineHitboxFace(
                relativeHit.x(),
                relativeHit.y() - hitbox.height / 2.0,
                relativeHit.z(),
                hitbox
            ),
            BlockPos(pos.blockX(), pos.blockY(), pos.blockZ()),
            false
        )
        val packet = ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hitResult, 0, System.currentTimeMillis())
        event.player.packetHandler?.injectIncoming(packet)
    }
    
    private fun determineHitboxFace(x: Double, y: Double, z: Double, hitbox: HitboxCuboid) =
        BlockFaceUtils.determineBlockFace(x / hitbox.width, y / hitbox.height, z / hitbox.width).nmsDirection
    
    private fun setMetadata(data: ItemDisplayMetadata, model: DisplayEntityBlockModelData.Model) {
        // TODO: proper light level
        data.brightnessOverride = if (info.collider.material.requiresLight) Brightness(15, 15) else null
        
        data.itemStack = model.itemStack
        data.transform = model.transform
    }
    
}
