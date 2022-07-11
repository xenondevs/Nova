@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.util

import net.minecraft.core.NonNullList
import net.minecraft.core.Rotations
import net.minecraft.network.protocol.Packet
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.dedicated.DedicatedServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property
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
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.world.BlockPos
import java.util.concurrent.atomic.AtomicInteger
import net.minecraft.core.BlockPos as MojangBlockPos
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

val Location.blockPos: MojangBlockPos
    get() = MojangBlockPos(blockX, blockY, blockZ)

val BlockPos.nmsPos: MojangBlockPos
    get() = MojangBlockPos(x, y, z)

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

fun MojangBlockPos.toNovaPos(world: World): BlockPos =
    BlockPos(world, x, y, z)

fun Player.send(vararg packets: Packet<*>) {
    val connection = connection
    packets.forEach { connection.send(it) }
}

fun Rotations.copy(x: Float? = null, y: Float? = null, z: Float? = null) =
    Rotations(x ?: this.x, y ?: this.y, z ?: this.z)

fun Rotations.add(x: Float, y: Float, z: Float) =
    Rotations(this.x + x, this.y + y, this.z + z)

val minecraftServer: DedicatedServer = (Bukkit.getServer() as CraftServer).server

val serverTick: Int
    get() = minecraftServer.tickCount

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

fun <T : Comparable<T>> BlockState.hasProperty(property: Property<T>, value: T): Boolean {
    return hasProperty(property) && values[property] == value
}

fun BlockPos.setBlockStateSilently(blockState: BlockState) {
    world.serverLevel.setBlock(nmsPos, blockState, 1024)
}

object NMSUtils {
    
    val ENTITY_COUNTER = ReflectionUtils.getField(
        MojangEntity::class.java,
        true,
        "SRF(net.minecraft.world.entity.Entity ENTITY_COUNTER)"
    ).get(null) as AtomicInteger
    
}
