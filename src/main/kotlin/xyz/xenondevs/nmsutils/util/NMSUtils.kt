package xyz.xenondevs.nmsutils.util

import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.chat.ComponentSerializer
import net.minecraft.core.Registry
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.item.Item
import net.minecraft.world.item.alchemy.Potion
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import org.bukkit.*
import org.bukkit.craftbukkit.v1_19_R1.CraftServer
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_19_R1.potion.CraftPotionUtil
import org.bukkit.craftbukkit.v1_19_R1.util.CraftChatMessage
import org.bukkit.craftbukkit.v1_19_R1.util.CraftMagicNumbers
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import net.minecraft.world.entity.EntityType as MojangEntityType
import net.minecraft.world.item.ItemStack as MojangItemStack

internal val DEDICATED_SERVER = (Bukkit.getServer() as CraftServer).server!!

internal val ItemStack.nmsStack: MojangItemStack
    get() = CraftItemStack.asNMSCopy(this)

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

internal val Tag<*>.tagKey: TagKey<*>
    get() = ReflectionRegistry.CRAFT_TAG_TAG_KEY_FIELD.get(this) as TagKey<*>

internal val Material.nmsItem: Item
    get() = CraftMagicNumbers.getItem(this)

internal val Material.nmsBlock: Block
    get() = CraftMagicNumbers.getBlock(this)

internal val World.resourceKey: ResourceKey<Level>
    get() = ResourceKey.create(Registry.DIMENSION_REGISTRY, name.resourceLocation!!)

internal fun Array<out BaseComponent>.toComponent(): Component {
    if (isEmpty()) return Component.empty()
    
    try {
        return CraftChatMessage.fromJSON(ComponentSerializer.toString(this))
    } catch (e: Exception) {
        throw IllegalArgumentException("Could not convert to Component: ${this.contentToString()}", e)
    }
}