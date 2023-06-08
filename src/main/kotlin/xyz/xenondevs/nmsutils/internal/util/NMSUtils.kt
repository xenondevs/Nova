@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nmsutils.internal.util

import io.netty.channel.ChannelFuture
import net.minecraft.advancements.critereon.ContextAwarePredicate
import net.minecraft.advancements.critereon.EntityPredicate
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.network.protocol.Packet
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.tags.TagKey
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.item.Item
import net.minecraft.world.item.alchemy.Potion
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.World
import org.bukkit.craftbukkit.v1_20_R1.CraftServer
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_20_R1.potion.CraftPotionUtil
import org.bukkit.craftbukkit.v1_20_R1.util.CraftMagicNumbers
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import net.minecraft.world.entity.Entity as MojangEntity
import net.minecraft.world.entity.EntityType as MojangEntityType
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

@Suppress("DEPRECATION")
internal val PotionEffectType.mobEffect: MobEffect
    get() = MobEffect.byId(id)!!

internal val PotionEffect.mobEffectInstance: MobEffectInstance
    get() = CraftPotionUtil.fromBukkit(this)

internal val PotionEffect.potion: Potion
    get() = Potion(mobEffectInstance)

internal val EntityType.nmsType: MojangEntityType<*>
    get() = MojangEntityType.byString(key.toString()).get()

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

internal val World.resourceKey: ResourceKey<Level>
    get() = ResourceKey.create(Registries.DIMENSION, name.resourceLocation!!)

internal val Player.serverPlayer: ServerPlayer
    get() = (this as CraftPlayer).handle

internal val Player.connection: ServerGamePacketListenerImpl
    get() = serverPlayer.connection

internal fun Player.send(vararg packets: Packet<*>) {
    val connection = connection
    packets.forEach { connection.send(it) }
}

internal fun <T> TagKey(key: ResourceKey<T>): TagKey<T> {
    return TagKey.create(ResourceKey.createRegistryKey(key.registry()), key.location())
}

internal fun EntityPredicate.asContextAwarePredicate(): ContextAwarePredicate = EntityPredicate.wrap(this)