package xyz.xenondevs.nova.util

import com.mojang.authlib.GameProfile
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.FluidTags
import net.minecraft.world.effect.MobEffectInstance
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.craftbukkit.v1_19_R1.CraftServer
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import xyz.xenondevs.nova.util.data.NBTUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import net.minecraft.world.entity.Entity as MojangEntity
import net.minecraft.world.entity.EntityType as NMSEntityType

fun Player.swingHand(hand: EquipmentSlot) {
    when (hand) {
        EquipmentSlot.HAND -> swingMainHand()
        EquipmentSlot.OFF_HAND -> swingOffHand()
        else -> throw IllegalArgumentException("EquipmentSlot is not a hand")
    }
}

fun Entity.teleport(modifyLocation: Location.() -> Unit) {
    val location = location
    location.modifyLocation()
    teleport(location)
}

val Entity.localizedName: String?
    get() = (this as CraftEntity).handle.type.descriptionId

val Entity.eyeInWater: Boolean
    get() = (this as CraftEntity).handle.isEyeInFluid(FluidTags.WATER)

object EntityUtils {
    
    /**
     * Gets a list of all passengers of this [Entity], including passengers of passengers.
     */
    fun getAllPassengers(entity: Entity): List<Entity> {
        val entitiesToCheck = CopyOnWriteArrayList<Entity>().apply { add(entity) }
        val passengers = ArrayList<Entity>()
        
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
     * Serializes an [Entity] to a [ByteArray].
     *
     * @param remove If the serialized [Entity] should be removed from the world.
     * @param nbtModifier Called before the [CompoundTag] gets compressed to a [ByteArray] to allow modifications.
     */
    fun serialize(
        entity: Entity,
        remove: Boolean = false,
        nbtModifier: ((CompoundTag) -> CompoundTag)? = null
    ): ByteArray {
        // get nms entity
        val nmsEntity = entity.nmsEntity
        
        // serialize data to compound tag
        var compoundTag = nmsEntity.saveWithoutId(CompoundTag())
        
        // add id tag to compound tag to identify entity type
        compoundTag.putString("id", nmsEntity.encodeId)
        
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
     * Spawns an [Entity] based on serialized [data] and a [location].
     *
     * @param nbtModifier Called before the [Entity] gets spawned into the world to allow nbt modifications.
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
    
    fun createFakePlayer(location: Location, uuid: UUID, name: String): ServerPlayer {
        val server = (Bukkit.getServer() as CraftServer).server
        val world = location.world!!.serverLevel
        val gameProfile = GameProfile(uuid, name)
        val serverPlayer = object : ServerPlayer(server, world, gameProfile, null) {
            override fun onEffectAdded(mobeffect: MobEffectInstance?, entity: MojangEntity?) = Unit
        }
        serverPlayer.advancements.stopListening()
        return serverPlayer
    }
    
}