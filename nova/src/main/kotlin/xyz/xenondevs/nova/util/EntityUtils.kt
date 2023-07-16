package xyz.xenondevs.nova.util

import com.mojang.authlib.GameProfile
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.FluidTags
import net.minecraft.world.effect.MobEffectInstance
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.craftbukkit.v1_20_R1.CraftServer
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity
import xyz.xenondevs.nova.item.behavior.Damageable
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.VanillaToolCategory
import xyz.xenondevs.nova.util.data.NBTUtils
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.world.block.logic.`break`.BlockBreaking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import net.minecraft.world.entity.Entity as MojangEntity
import net.minecraft.world.entity.EntityType as NMSEntityType
import net.minecraft.world.entity.EquipmentSlot as MojangEquipmentSlot
import net.minecraft.world.entity.LivingEntity as MojangLivingEntity
import org.bukkit.entity.Entity as BukkitEntity
import org.bukkit.entity.LivingEntity as BukkitLivingEntity
import org.bukkit.entity.Player as BukkitPlayer
import org.bukkit.inventory.EquipmentSlot as BukkitEquipmentSlot

/**
 * The current block destroy progress of the player.
 * Between 0 and 1 or null if the player is not breaking a block at the moment.
 */
val BukkitPlayer.destroyProgress: Double?
    get() = BlockBreaking.getBreaker(this)?.progress?.coerceAtMost(1.0)

/**
 * Damages the item in the [entity's][BukkitLivingEntity] main hand by [damage] amount.
 */
fun BukkitLivingEntity.damageItemInMainHand(damage: Int = 1) {
    if (damage <= 0)
        return
    val serverPlayer = nmsEntity as MojangLivingEntity
    Damageable.damageAndBreak(serverPlayer.mainHandItem, damage) { serverPlayer.broadcastBreakEvent(MojangEquipmentSlot.MAINHAND) }
}

/**
 * Damages the item in the [entity's][BukkitLivingEntity] offhand by [damage] amount.
 */
fun BukkitLivingEntity.damageItemInOffHand(damage: Int = 1) {
    if (damage <= 0)
        return
    val serverPlayer = nmsEntity as MojangLivingEntity
    Damageable.damageAndBreak(serverPlayer.offhandItem, damage) { serverPlayer.broadcastBreakEvent(MojangEquipmentSlot.OFFHAND) }
}

/**
 * Damages the item in the specified [hand] by [damage] amount.
 */
fun BukkitLivingEntity.damageItemInHand(hand: BukkitEquipmentSlot, damage: Int = 1) {
    when (hand) {
        BukkitEquipmentSlot.HAND -> damageItemInMainHand(damage)
        BukkitEquipmentSlot.OFF_HAND -> damageItemInOffHand(damage)
        else -> throw IllegalArgumentException("Not a hand: $hand")
    }
}

/**
 * Damages the tool in the [entity's][BukkitLivingEntity] main hand as if they've broken a block.
 */
fun BukkitLivingEntity.damageToolBreakBlock() = damageToolInMainHand(Damageable::itemDamageOnBreakBlock, VanillaToolCategory::itemDamageOnBreakBlock)

/**
 * Damages the tool in the [entity's][BukkitLivingEntity] main hand as if they've attack an entity.
 */
fun BukkitLivingEntity.damageToolAttackEntity() = damageToolInMainHand(Damageable::itemDamageOnAttackEntity, VanillaToolCategory::itemDamageOnAttackEntity)

private inline fun BukkitLivingEntity.damageToolInMainHand(getNovaDamage: (Damageable) -> Int, getVanillaDamage: (VanillaToolCategory) -> Int) {
    val itemStack = (nmsEntity as MojangLivingEntity).mainHandItem
    val novaItem = itemStack.novaItem
    
    val damage: Int
    if (novaItem != null) {
        val damageable = novaItem.getBehaviorOrNull<Damageable>() ?: return
        damage = getNovaDamage(damageable)
    } else {
        val toolCategory = ToolCategory.ofItem(itemStack.bukkitMirror) as? VanillaToolCategory ?: return
        damage = getVanillaDamage(toolCategory)
    }
    
    damageItemInMainHand(damage)
}

/**
 * Teleports the [BukkitEntity] after modifying its location using the [modifyLocation] lambda.
 */
fun BukkitEntity.teleport(modifyLocation: Location.() -> Unit) {
    val location = location
    location.modifyLocation()
    teleport(location)
}

/**
 * The translation key for the name of this [BukkitEntity].
 */
val BukkitEntity.localizedName: String?
    get() = (this as CraftEntity).handle.type.descriptionId

/**
 * If the [Entity's][BukkitEntity] eye is underwater.
 */
val BukkitEntity.eyeInWater: Boolean
    get() = (this as CraftEntity).handle.isEyeInFluid(FluidTags.WATER)

object EntityUtils {
    
    internal val DUMMY_PLAYER = createFakePlayer(Location(Bukkit.getWorlds()[0], 0.0, 0.0, 0.0), UUID.randomUUID(), "Nova Dummy Player")
    
    /**
     * Gets a list of all passengers of this [BukkitEntity], including passengers of passengers.
     */
    fun getAllPassengers(entity: BukkitEntity): List<BukkitEntity> {
        val entitiesToCheck = CopyOnWriteArrayList<BukkitEntity>().apply { add(entity) }
        val passengers = ArrayList<BukkitEntity>()
        
        while (entitiesToCheck.isNotEmpty()) {
            for (entityToCheck in entitiesToCheck) {
                entitiesToCheck += entityToCheck.passengers
                passengers += entityToCheck.passengers
                
                entitiesToCheck -= entityToCheck
            }
        }
        
        return passengers
    }
    
    /**
     * Serializes an [BukkitEntity] to a [ByteArray].
     *
     * @param remove If the serialized [BukkitEntity] should be removed from the world.
     * @param nbtModifier Called before the [CompoundTag] gets compressed to a [ByteArray] to allow modifications.
     */
    fun serialize(
        entity: BukkitEntity,
        remove: Boolean = false,
        nbtModifier: ((CompoundTag) -> CompoundTag)? = null
    ): ByteArray {
        // get nms entity
        val nmsEntity = entity.nmsEntity
        
        // serialize data to compound tag
        var compoundTag = nmsEntity.saveWithoutId(CompoundTag())
        
        // add id tag to compound tag to identify entity type
        compoundTag.putString("id", nmsEntity.encodeId!!)
        
        // modify nbt data
        if (nbtModifier != null) compoundTag = nbtModifier.invoke(compoundTag)
        
        // write data to byte array
        val stream = ByteArrayOutputStream()
        NbtIo.writeCompressed(compoundTag, stream)
        val data = stream.toByteArray()
        
        if (remove) {
            getAllPassengers(entity).forEach { it.remove() }
            entity.remove()
        }
        
        return data
    }
    
    /**
     * Spawns an [BukkitEntity] based on serialized [data] and a [location].
     *
     * @param nbtModifier Called before the [BukkitEntity] gets spawned into the world to allow nbt modifications.
     */
    fun deserializeAndSpawn(
        data: ByteArray,
        location: Location,
        nbtModifier: ((CompoundTag) -> CompoundTag)? = null
    ): MojangEntity {
        // get world
        val world = location.world!!
        val level = world.serverLevel
        
        // read data to compound tag
        var compoundTag = NbtIo.readCompressed(ByteArrayInputStream(data))
        
        // set new location in nbt data
        compoundTag.put("Pos", NBTUtils.createDoubleList(location.x, location.y, location.z))
        
        // set new rotation in nbt data
        compoundTag.put("Rotation", NBTUtils.createFloatList(location.yaw, location.pitch))
        
        // modify nbt data
        if (nbtModifier != null) compoundTag = nbtModifier.invoke(compoundTag)
        
        // deserialize compound tag to entity
        return NMSEntityType.loadEntityRecursive(compoundTag, level) { entity ->
            // assign new uuid
            entity.uuid = UUID.randomUUID()
            
            // add entity to world
            level.addWithUUID(entity)
            entity
        }!!
    }
    
    /**
     * Creates a fake [ServerPlayer] object.
     */
    fun createFakePlayer(
        location: Location,
        uuid: UUID = UUID.randomUUID(),
        name: String = "Nova FakePlayer",
        hasEvents: Boolean = false
    ): ServerPlayer {
        val server = (Bukkit.getServer() as CraftServer).server
        val world = location.world!!.serverLevel
        val gameProfile = GameProfile(uuid, name)
        return FakePlayer(server, world, gameProfile, hasEvents)
    }
    
}

class FakePlayer(
    server: MinecraftServer,
    level: ServerLevel,
    profile: GameProfile,
    val hasEvents: Boolean
) : ServerPlayer(server, level, profile) {
    
    init {
        advancements.stopListening()
    }
    
    override fun onEffectAdded(effect: MobEffectInstance, source: MojangEntity?) {
        // empty
    }
    
}