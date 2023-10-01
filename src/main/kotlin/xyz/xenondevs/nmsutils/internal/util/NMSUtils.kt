@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nmsutils.internal.util

import io.netty.channel.ChannelFuture
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.Packet
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.craftbukkit.v1_20_R2.CraftServer
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftEntity
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer
import org.bukkit.craftbukkit.v1_20_R2.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_20_R2.util.CraftMagicNumbers
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import net.minecraft.world.entity.Entity as MojangEntity
import net.minecraft.world.item.ItemStack as MojangStack

internal val DEDICATED_SERVER = (Bukkit.getServer() as CraftServer).server!!

internal val MinecraftServer.channels: List<ChannelFuture>
    get() = ReflectionRegistry.SERVER_CONNECTION_LISTENER_CHANNELS_FIELD.get(this.connection) as List<ChannelFuture>

internal val ItemStack.nmsStack: MojangStack
    get() = CraftItemStack.asNMSCopy(this)

internal val MojangStack.bukkitStack: ItemStack
    get() = CraftItemStack.asBukkitCopy(this)

internal val NamespacedKey.resourceLocation: ResourceLocation
    get() = ResourceLocation(toString())

internal val String.resourceLocation: ResourceLocation?
    get() = ResourceLocation.tryParse(this)

internal val Entity.nmsEntity: MojangEntity
    get() = (this as CraftEntity).handle

internal val Location.blockPos: BlockPos
    get() = BlockPos(blockX, blockY, blockZ)

internal val Tag<*>.tagKey: TagKey<*>
    get() = ReflectionRegistry.CRAFT_TAG_TAG_KEY_FIELD.get(this) as TagKey<*>

internal val Material.nmsItem: Item
    get() = CraftMagicNumbers.getItem(this)

internal val Material.nmsBlock: Block
    get() = CraftMagicNumbers.getBlock(this)

internal val Player.serverPlayer: ServerPlayer
    get() = (this as CraftPlayer).handle

internal val Player.connection: ServerGamePacketListenerImpl
    get() = serverPlayer.connection

internal fun Player.send(vararg packets: Packet<*>) {
    val connection = connection
    packets.forEach { connection.send(it) }
}