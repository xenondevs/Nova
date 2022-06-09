@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.util

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFuture
import net.minecraft.core.BlockPos
import net.minecraft.core.NonNullList
import net.minecraft.core.Rotations
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.dedicated.DedicatedServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.InteractionHand
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.craftbukkit.v1_19_R1.CraftServer
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.network.PacketManager
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import java.util.concurrent.atomic.AtomicInteger
import net.minecraft.world.entity.Entity as MojangEntity
import net.minecraft.world.item.ItemStack as NMSItemStack

val Entity.nmsEntity: MojangEntity
    get() = (this as CraftEntity).handle

val Player.serverPlayer: ServerPlayer
    get() = (this as CraftPlayer).handle

val ItemStack.nmsStack: NMSItemStack
    get() = CraftItemStack.asNMSCopy(this)

val NMSItemStack.bukkitStack: ItemStack
    get() = CraftItemStack.asBukkitCopy(this)

val Location.blockPos: BlockPos
    get() = BlockPos(blockX, blockY, blockZ)

val World.serverLevel: ServerLevel
    get() = (this as CraftWorld).handle

val Player.connection: ServerGamePacketListenerImpl
    get() = serverPlayer.connection

val NamespacedKey.resourceLocation: ResourceLocation
    get() = ResourceLocation(toString())

@Suppress("DEPRECATION")
val ResourceLocation.namespacedKey: NamespacedKey
    get() = NamespacedKey(namespace, path)

val InteractionHand.bukkitSlot: EquipmentSlot
    get() = when (this) {
        InteractionHand.MAIN_HAND -> EquipmentSlot.HAND
        InteractionHand.OFF_HAND -> EquipmentSlot.OFF_HAND
    }

fun Player.send(vararg packets: Packet<*>) {
    val connection = connection
    packets.forEach { connection.send(it) }
}

fun Player.send(vararg bufs: FriendlyByteBuf, retain: Boolean = true, flush: Boolean = true) {
    val queue = PacketManager.playerHandlers[name]?.queue ?: return
    bufs.forEach {
        if (retain) it.retain()
        queue += it
    }
    
    if (flush) connection.connection.channel.flush()
}

fun Rotations.copy(x: Float? = null, y: Float? = null, z: Float? = null) =
    Rotations(x ?: this.x, y ?: this.y, z ?: this.z)

fun Rotations.add(x: Float, y: Float, z: Float) =
    Rotations(this.x + x, this.y + y, this.z + z)

val minecraftServer: DedicatedServer = (Bukkit.getServer() as CraftServer).server

val MinecraftServer.channels: List<ChannelFuture>
    get() = ReflectionRegistry.SERVER_CONNECTION_LISTENER_CHANNELS_FIELD.get(this.connection) as List<ChannelFuture>

val serverTick: Int
    get() = minecraftServer.tickCount

fun ClientboundPlaceGhostRecipePacket(containerId: Int, resourceLocation: String): ClientboundPlaceGhostRecipePacket {
    val buffer = FriendlyByteBuf(Unpooled.buffer())
    buffer.writeByte(containerId)
    buffer.writeResourceLocation(ResourceLocation(resourceLocation))
    return ClientboundPlaceGhostRecipePacket(buffer)
}

fun ClientboundSetPassengersPacket(vehicle: Int, passengers: IntArray): ClientboundSetPassengersPacket {
    val buffer = FriendlyByteBuf(Unpooled.buffer())
    buffer.writeVarInt(vehicle)
    buffer.writeVarIntArray(passengers)
    return ClientboundSetPassengersPacket(buffer)
}

fun ClientboundRotateHeadPacket(entity: Int, yaw: Float): ClientboundRotateHeadPacket {
    val buffer = FriendlyByteBuf(Unpooled.buffer())
    buffer.writeVarInt(entity)
    buffer.writeByte(yaw.toPackedByte().toInt())
    return ClientboundRotateHeadPacket(buffer)
}

fun <E> NonNullList(list: List<E>, default: E? = null): NonNullList<E> {
    val nonNullList: NonNullList<E>
    if (default == null) {
        nonNullList = NonNullList.createWithCapacity(list.size)
        nonNullList.addAll(list)
    } else {
        nonNullList = NonNullList.withSize(list.size, default)
        list.forEachIndexed { index, e -> nonNullList[index] = e }
    }
    
    return nonNullList
}

object NMSUtils {
    
    val ENTITY_COUNTER = ReflectionUtils.getField(
        MojangEntity::class.java,
        true,
        "SRF(net.minecraft.world.entity.Entity ENTITY_COUNTER)"
    ).get(null) as AtomicInteger
    
}
