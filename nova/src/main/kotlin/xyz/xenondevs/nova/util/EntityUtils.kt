package xyz.xenondevs.nova.util

import com.mojang.authlib.GameProfile
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ClientInformation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.FluidTags
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.item.ItemEntity
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.collections.firstInstanceOfOrNull
import xyz.xenondevs.nova.util.data.NBTUtils
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.logic.`break`.BlockBreaking
import xyz.xenondevs.nova.world.item.behavior.Damageable
import xyz.xenondevs.nova.world.item.tool.ToolCategory
import xyz.xenondevs.nova.world.item.tool.VanillaToolCategory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random
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
    val nmsEntity = nmsEntity as MojangLivingEntity
    nmsEntity.mainHandItem.hurtAndBreak(damage, nmsEntity, MojangEquipmentSlot.MAINHAND)
}

/**
 * Damages the item in the [entity's][BukkitLivingEntity] offhand by [damage] amount.
 */
fun BukkitLivingEntity.damageItemInOffHand(damage: Int = 1) {
    if (damage <= 0)
        return
    val nmsEntity = nmsEntity as MojangLivingEntity
    nmsEntity.offhandItem.hurtAndBreak(damage, nmsEntity, MojangEquipmentSlot.OFFHAND)
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
        val toolCategory = ToolCategory.ofItem(itemStack.asBukkitMirror()).firstInstanceOfOrNull<VanillaToolCategory>() ?: return
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
    private val DEFAULT_DESERIALIZATION_DISALLOWED_ENTITY_TYPES: Set<EntityType> = buildSet { 
        add(EntityType.COMMAND_BLOCK_MINECART)
        add(EntityType.FALLING_BLOCK) // command block falling block (for good measure, command doesn't seem to be there after landing)
    }
    
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
     * Creates not-spawned [item entities][ItemEntity] based on the specified [items] and [pos].
     */
    fun createBlockDropItemEntities(pos: BlockPos, items: Iterable<ItemStack>): List<ItemEntity> =
        items.map {
            ItemEntity(
                pos.world.serverLevel,
                pos.x + 0.5 + Random.nextDouble(-0.25, 0.25),
                pos.y + 0.5 + Random.nextDouble(-0.25, 0.25),
                pos.z + 0.5 + Random.nextDouble(-0.25, 0.25),
                it.unwrap().copy()
            ).apply(ItemEntity::setDefaultPickUpDelay)
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
    
    
    // TODO: merge functions in 0.21
    /**
     * Spawns an [BukkitEntity] based on serialized [data] and a [location].
     *
     * @param nbtModifier Called before the [BukkitEntity] gets spawned into the world to allow nbt modifications.
     */
    @Deprecated("Use deserializeAndSpawn with disallowedEntityTypes parameter instead.", ReplaceWith("deserializeAndSpawn(data, location, spawnReason, disallowedEntityTypes, nbtModifier)"))
    fun deserializeAndSpawn(
        data: ByteArray,
        location: Location,
        spawnReason: EntitySpawnReason = EntitySpawnReason.MOB_SUMMONED,
        nbtModifier: ((CompoundTag) -> CompoundTag)? = null
    ): MojangEntity = deserializeAndSpawn(data, location, spawnReason, DEFAULT_DESERIALIZATION_DISALLOWED_ENTITY_TYPES, nbtModifier)!!
    
    /**
     * Spawns an [BukkitEntity] based on serialized [data] and a [location], skipping
     * all entities whose [EntityType] or their passengers are in the [disallowedEntityTypes] set.
     * 
     * @param disallowedEntityTypes A set of [EntityType]s that should not be spawned.
     * @param nbtModifier Called before the [BukkitEntity] gets spawned into the world to allow nbt modifications.
     */
    fun deserializeAndSpawn(
        data: ByteArray,
        location: Location,
        spawnReason: EntitySpawnReason = EntitySpawnReason.MOB_SUMMONED,
        disallowedEntityTypes: Set<EntityType> = DEFAULT_DESERIALIZATION_DISALLOWED_ENTITY_TYPES,
        nbtModifier: ((CompoundTag) -> CompoundTag)? = null
    ): MojangEntity? {
        // get world
        val world = location.world!!
        val level = world.serverLevel
        
        // read data to compound tag
        var compoundTag = NbtIo.readCompressed(ByteArrayInputStream(data), NbtAccounter.unlimitedHeap())
        
        // set new location in nbt data
        compoundTag.put("Pos", NBTUtils.createDoubleList(location.x, location.y, location.z))
        
        // set new rotation in nbt data
        compoundTag.put("Rotation", NBTUtils.createFloatList(location.yaw, location.pitch))
        
        // modify nbt data
        if (nbtModifier != null) compoundTag = nbtModifier.invoke(compoundTag) // TODO: passengers
        
        val entities = ArrayList<MojangEntity>()
        NMSEntityType.loadEntityRecursive(compoundTag, level, spawnReason) { entity ->
            // assign new uuid
            entity.uuid = UUID.randomUUID()
            
            // (deferred) add entity to world
            entities += entity
            entity
        }
        
        if (entities.isNotEmpty() && entities.none { it.bukkitEntity.type in disallowedEntityTypes }) {
            entities.forEach(level::addWithUUID)
            return entities[0]
        } else {
            return null
        }
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
        val world = location.world!!.serverLevel
        val gameProfile = GameProfile(uuid, name)
        return FakePlayer(MINECRAFT_SERVER, world, gameProfile, hasEvents)
    }
    
}

class FakePlayer(
    server: MinecraftServer,
    level: ServerLevel,
    profile: GameProfile,
    val hasEvents: Boolean
) : ServerPlayer(server, level, profile, ClientInformation.createDefault()) {
    
    init {
        advancements.stopListening()
    }
    
    override fun onEffectAdded(effect: MobEffectInstance, source: MojangEntity?) {
        // empty
    }
    
}