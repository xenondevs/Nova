@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nmsutils.internal.util

import io.netty.channel.ChannelFuture
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.chat.ComponentSerializer
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
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
import org.bukkit.*
import org.bukkit.craftbukkit.v1_19_R2.CraftServer
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftEntity
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_19_R2.potion.CraftPotionUtil
import org.bukkit.craftbukkit.v1_19_R2.util.CraftChatMessage
import org.bukkit.craftbukkit.v1_19_R2.util.CraftMagicNumbers
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

internal fun Component.toBaseComponentArray(): Array<BaseComponent> {
    try {
        return ComponentSerializer.parse(CraftChatMessage.toJSON(this))
    } catch (e: Exception) {
        throw IllegalArgumentException("Could not convert to BaseComponent array: $this", e)
    }
}

internal fun Array<out BaseComponent>.toComponent(): Component {
    if (isEmpty()) return Component.empty()
    
    try {
        return CraftChatMessage.fromJSON(ComponentSerializer.toString(this))
    } catch (e: Exception) {
        throw IllegalArgumentException("Could not convert to Component: ${this.contentToString()}", e)
    }
}