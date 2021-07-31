package xyz.xenondevs.nova.util

import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.Packet
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.phys.Vec3
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

val Entity.nmsEntity: net.minecraft.world.entity.Entity
    get() = (this as CraftEntity).handle

val Player.serverPlayer: ServerPlayer
    get() = (this as CraftPlayer).handle

val ItemStack.nmsStack: net.minecraft.world.item.ItemStack
    get() = ReflectionRegistry.CB_CRAFT_ITEM_STACK_AS_NMS_COPY_METHOD.invoke(null, this) as net.minecraft.world.item.ItemStack

val Location.blockPos: BlockPos
    get() = BlockPos(blockX, blockY, blockZ)

val Location.vec3: Vec3
    get() = Vec3(x, y, z)

val World.serverLevel: ServerLevel
    get() = ReflectionRegistry.CB_CRAFT_WORLD_GET_HANDLE_METHOD.invoke(this) as ServerLevel

val Player.connection: ServerGamePacketListenerImpl
    get() = serverPlayer.connection

fun Player.send(vararg packets: Packet<*>) {
    val connection = connection
    packets.forEach { connection.send(it) }
}
