@file:Suppress("unused")

package xyz.xenondevs.nova.util

import com.mojang.datafixers.util.Either
import io.netty.buffer.Unpooled
import net.kyori.adventure.key.Key
import net.minecraft.core.Direction
import net.minecraft.core.Holder
import net.minecraft.core.HolderGetter
import net.minecraft.core.MappedRegistry
import net.minecraft.core.NonNullList
import net.minecraft.core.RegistrationInfo
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.core.Rotations
import net.minecraft.core.WritableRegistry
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.protocol.Packet
import net.minecraft.resources.RegistryOps.RegistryInfoLookup
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.dedicated.DedicatedServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.server.players.PlayerList
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.block.data.CraftBlockData
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.craftbukkit.util.CraftMagicNumbers
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Pose
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.id
import xyz.xenondevs.nova.patch.impl.playerlist.BroadcastPacketPatch
import xyz.xenondevs.nova.patch.impl.worldgen.chunksection.LevelChunkSectionWrapper
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.world.BlockPos
import java.util.Optional
import java.util.concurrent.atomic.AtomicInteger
import kotlin.jvm.optionals.getOrNull
import net.minecraft.core.BlockPos as MojangBlockPos
import net.minecraft.world.entity.Entity as MojangEntity
import net.minecraft.world.entity.EquipmentSlot as MojangEquipmentSlot
import net.minecraft.world.entity.Pose as MojangPose
import net.minecraft.world.entity.ai.attributes.Attribute as MojangAttribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier as MojangAttributeModifier
import net.minecraft.world.entity.player.Player as MojangPlayer
import net.minecraft.world.item.Item as MojangItem
import net.minecraft.world.item.ItemStack as MojangStack
import net.minecraft.world.level.block.Block as MojangBlock

val MINECRAFT_SERVER: DedicatedServer by lazy { (Bukkit.getServer() as CraftServer).server }
val REGISTRY_ACCESS: RegistryAccess by lazy { MINECRAFT_SERVER.registryAccess() }
val DATA_VERSION: Int by lazy { CraftMagicNumbers.INSTANCE.dataVersion }

val Entity.nmsEntity: MojangEntity
    get() = (this as CraftEntity).handle

val Player.serverPlayer: ServerPlayer
    get() = (this as CraftPlayer).handle

@Deprecated("All bukkit stacks now wrap an nms stack", ReplaceWith("unwrap().copy()", "xyz.xenondevs.nova.util.unwrap"))
val ItemStack?.nmsCopy: MojangStack
    get() = unwrap().copy()

fun ItemStack?.unwrap(): MojangStack =
    this?.let(CraftItemStack::unwrap) ?: MojangStack.EMPTY

val BlockData.nmsBlockState: BlockState
    get() = (this as CraftBlockData).state

val BlockState.bukkitBlockData: BlockData
    get() = CraftBlockData.fromData(this)

val Location.blockPos: MojangBlockPos
    get() = MojangBlockPos(blockX, blockY, blockZ)

val Location.vec3: Vec3
    get() = Vec3(x, y, z)

val Vector.vec3: Vec3
    get() = Vec3(x, y, z)

val MojangBlockPos.vec3: Vec3
    get() = Vec3(x.toDouble(), y.toDouble(), z.toDouble())

val MojangBlockPos.center: Vec3
    get() = Vec3(x + 0.5, y + 0.5, z + 0.5)

val World.serverLevel: ServerLevel
    get() = (this as CraftWorld).handle

val Chunk.levelChunk: LevelChunk
    get() = world.serverLevel.getChunk(x, z)

val Player.connection: ServerGamePacketListenerImpl
    get() = serverPlayer.connection

val NamespacedKey.resourceLocation: ResourceLocation
    get() = ResourceLocation.fromNamespaceAndPath(namespace, key)

val ResourceLocation.namespacedKey: NamespacedKey
    get() = NamespacedKey(namespace, path)

fun <C: ResourceType> ResourceLocation.toResourcePath(type: C): ResourcePath<C> =
    ResourcePath(type, namespace, path)

fun Key.toResourceLocation(): ResourceLocation =
    ResourceLocation.fromNamespaceAndPath(namespace(), value())

fun ResourceLocation.toKey(): Key =
    Key.key(namespace, path)

val EquipmentSlot.nmsInteractionHand: InteractionHand
    get() = when (this) {
        EquipmentSlot.HAND -> InteractionHand.MAIN_HAND
        EquipmentSlot.OFF_HAND -> InteractionHand.OFF_HAND
        else -> throw UnsupportedOperationException("Not a hand: $this")
    }

val EquipmentSlot.nmsEquipmentSlot: MojangEquipmentSlot
    get() = when (this) {
        EquipmentSlot.HAND -> MojangEquipmentSlot.MAINHAND
        EquipmentSlot.OFF_HAND -> MojangEquipmentSlot.OFFHAND
        EquipmentSlot.FEET -> MojangEquipmentSlot.FEET
        EquipmentSlot.LEGS -> MojangEquipmentSlot.LEGS
        EquipmentSlot.CHEST -> MojangEquipmentSlot.CHEST
        EquipmentSlot.HEAD -> MojangEquipmentSlot.HEAD
        EquipmentSlot.BODY -> MojangEquipmentSlot.BODY
    }

val MojangEquipmentSlot.bukkitEquipmentSlot: EquipmentSlot
    get() = when (this) {
        MojangEquipmentSlot.MAINHAND -> EquipmentSlot.HAND
        MojangEquipmentSlot.OFFHAND -> EquipmentSlot.OFF_HAND
        MojangEquipmentSlot.FEET -> EquipmentSlot.FEET
        MojangEquipmentSlot.LEGS -> EquipmentSlot.LEGS
        MojangEquipmentSlot.CHEST -> EquipmentSlot.CHEST
        MojangEquipmentSlot.HEAD -> EquipmentSlot.HEAD
        MojangEquipmentSlot.BODY -> EquipmentSlot.BODY
    }

val MojangEquipmentSlot.nmsInteractionHand: InteractionHand
    get() = when (this) {
        MojangEquipmentSlot.MAINHAND -> InteractionHand.MAIN_HAND
        MojangEquipmentSlot.OFFHAND -> InteractionHand.OFF_HAND
        else -> throw UnsupportedOperationException("Not a hand: $this")
    }

val InteractionHand.bukkitEquipmentSlot: EquipmentSlot
    get() = when (this) {
        InteractionHand.MAIN_HAND -> EquipmentSlot.HAND
        InteractionHand.OFF_HAND -> EquipmentSlot.OFF_HAND
    }

val InteractionHand.nmsEquipmentSlot: MojangEquipmentSlot
    get() = when (this) {
        InteractionHand.MAIN_HAND -> MojangEquipmentSlot.MAINHAND
        InteractionHand.OFF_HAND -> MojangEquipmentSlot.OFFHAND
    }

val BlockFace.nmsDirection: Direction
    get() = when (this) {
        BlockFace.NORTH -> Direction.NORTH
        BlockFace.EAST -> Direction.EAST
        BlockFace.SOUTH -> Direction.SOUTH
        BlockFace.WEST -> Direction.WEST
        BlockFace.UP -> Direction.UP
        BlockFace.DOWN -> Direction.DOWN
        else -> throw UnsupportedOperationException()
    }

val Direction.blockFace: BlockFace
    get() = when (this) {
        Direction.NORTH -> BlockFace.NORTH
        Direction.EAST -> BlockFace.EAST
        Direction.SOUTH -> BlockFace.SOUTH
        Direction.WEST -> BlockFace.WEST
        Direction.UP -> BlockFace.UP
        Direction.DOWN -> BlockFace.DOWN
    }

val Attribute.nmsAttribute: MojangAttribute
    get() = when (this) {
        Attribute.MAX_HEALTH -> Attributes.MAX_HEALTH
        Attribute.FOLLOW_RANGE -> Attributes.FOLLOW_RANGE
        Attribute.KNOCKBACK_RESISTANCE -> Attributes.KNOCKBACK_RESISTANCE
        Attribute.MOVEMENT_SPEED -> Attributes.MOVEMENT_SPEED
        Attribute.FLYING_SPEED -> Attributes.FLYING_SPEED
        Attribute.ATTACK_DAMAGE -> Attributes.ATTACK_DAMAGE
        Attribute.ATTACK_KNOCKBACK -> Attributes.ATTACK_KNOCKBACK
        Attribute.ATTACK_SPEED -> Attributes.ATTACK_SPEED
        Attribute.ARMOR -> Attributes.ARMOR
        Attribute.ARMOR_TOUGHNESS -> Attributes.ARMOR_TOUGHNESS
        Attribute.FALL_DAMAGE_MULTIPLIER -> Attributes.FALL_DAMAGE_MULTIPLIER
        Attribute.LUCK -> Attributes.LUCK
        Attribute.MAX_ABSORPTION -> Attributes.MAX_ABSORPTION
        Attribute.SAFE_FALL_DISTANCE -> Attributes.SAFE_FALL_DISTANCE
        Attribute.SCALE -> Attributes.SCALE
        Attribute.STEP_HEIGHT -> Attributes.STEP_HEIGHT
        Attribute.GRAVITY -> Attributes.GRAVITY
        Attribute.JUMP_STRENGTH -> Attributes.JUMP_STRENGTH
        Attribute.BURNING_TIME -> Attributes.BURNING_TIME
        Attribute.EXPLOSION_KNOCKBACK_RESISTANCE -> Attributes.EXPLOSION_KNOCKBACK_RESISTANCE
        Attribute.MOVEMENT_EFFICIENCY -> Attributes.MOVEMENT_EFFICIENCY
        Attribute.OXYGEN_BONUS -> Attributes.OXYGEN_BONUS
        Attribute.WATER_MOVEMENT_EFFICIENCY -> Attributes.WATER_MOVEMENT_EFFICIENCY
        Attribute.TEMPT_RANGE -> Attributes.TEMPT_RANGE
        Attribute.BLOCK_INTERACTION_RANGE -> Attributes.BLOCK_INTERACTION_RANGE
        Attribute.ENTITY_INTERACTION_RANGE -> Attributes.ENTITY_INTERACTION_RANGE
        Attribute.BLOCK_BREAK_SPEED -> Attributes.BLOCK_BREAK_SPEED
        Attribute.MINING_EFFICIENCY -> Attributes.MINING_EFFICIENCY
        Attribute.SNEAKING_SPEED -> Attributes.SNEAKING_SPEED
        Attribute.SUBMERGED_MINING_SPEED -> Attributes.SUBMERGED_MINING_SPEED
        Attribute.SWEEPING_DAMAGE_RATIO -> Attributes.SWEEPING_DAMAGE_RATIO
        Attribute.SPAWN_REINFORCEMENTS -> Attributes.SPAWN_REINFORCEMENTS_CHANCE
        else -> throw UnsupportedOperationException("Unknown attribute: $this")
    }.value()

val AttributeModifier.Operation.nmsOperation: MojangAttributeModifier.Operation
    get() = when (this) {
        AttributeModifier.Operation.ADD_NUMBER -> MojangAttributeModifier.Operation.ADD_VALUE
        AttributeModifier.Operation.ADD_SCALAR -> MojangAttributeModifier.Operation.ADD_MULTIPLIED_BASE
        AttributeModifier.Operation.MULTIPLY_SCALAR_1 -> MojangAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    }

val MojangPose.bukkitPose: Pose
    get() = when (this) {
        MojangPose.STANDING -> Pose.STANDING
        MojangPose.FALL_FLYING -> Pose.FALL_FLYING
        MojangPose.SLEEPING -> Pose.SLEEPING
        MojangPose.SWIMMING -> Pose.SWIMMING
        MojangPose.SPIN_ATTACK -> Pose.SPIN_ATTACK
        MojangPose.CROUCHING -> Pose.SNEAKING
        MojangPose.LONG_JUMPING -> Pose.LONG_JUMPING
        MojangPose.DYING -> Pose.DYING
        MojangPose.CROAKING -> Pose.CROAKING
        MojangPose.USING_TONGUE -> Pose.USING_TONGUE
        MojangPose.SITTING -> Pose.SITTING
        MojangPose.ROARING -> Pose.ROARING
        MojangPose.SNIFFING -> Pose.SNIFFING
        MojangPose.EMERGING -> Pose.EMERGING
        MojangPose.DIGGING -> Pose.DIGGING
        MojangPose.SLIDING -> Pose.SLIDING
        MojangPose.SHOOTING -> Pose.SHOOTING
        MojangPose.INHALING -> Pose.INHALING
    }

val Pose.nmsPose: MojangPose
    get() = when (this) {
        Pose.STANDING -> MojangPose.STANDING
        Pose.FALL_FLYING -> MojangPose.FALL_FLYING
        Pose.SLEEPING -> MojangPose.SLEEPING
        Pose.SWIMMING -> MojangPose.SWIMMING
        Pose.SPIN_ATTACK -> MojangPose.SPIN_ATTACK
        Pose.SNEAKING -> MojangPose.CROUCHING
        Pose.LONG_JUMPING -> MojangPose.LONG_JUMPING
        Pose.DYING -> MojangPose.DYING
        Pose.CROAKING -> MojangPose.CROAKING
        Pose.USING_TONGUE -> MojangPose.USING_TONGUE
        Pose.SITTING -> MojangPose.SITTING
        Pose.ROARING -> MojangPose.ROARING
        Pose.SNIFFING -> MojangPose.SNIFFING
        Pose.EMERGING -> MojangPose.EMERGING
        Pose.DIGGING -> MojangPose.DIGGING
        Pose.SLIDING -> MojangPose.SLIDING
        Pose.SHOOTING -> MojangPose.SHOOTING
        Pose.INHALING -> MojangPose.INHALING
    }

val Material.nmsBlock: MojangBlock
    get() = CraftMagicNumbers.getBlock(this)

val Material.nmsItem: MojangItem
    get() = CraftMagicNumbers.getItem(this)

val MojangBlock.bukkitMaterial: Material
    get() = CraftMagicNumbers.getMaterial(this)

val MojangItem.bukkitMaterial: Material
    get() = CraftMagicNumbers.getMaterial(this)

val Block.nmsState: BlockState
    get() = world.serverLevel.getBlockState(MojangBlockPos(x, y, z))

val BlockState.id: Int
    get() = MojangBlock.getId(this)

fun MojangBlockPos.toNovaPos(world: World): BlockPos =
    BlockPos(world, x, y, z)

fun Player.send(vararg packets: Packet<*>) {
    val connection = connection
    packets.forEach { connection.send(it) }
}

fun Player.send(packets: Iterable<Packet<*>>) {
    val connection = connection
    packets.forEach { connection.send(it) }
}

fun Packet<*>.sendTo(vararg players: Player) {
    players.forEach { it.send(this) }
}

fun Packet<*>.sendTo(players: Iterable<Player>) {
    players.forEach { it.send(this) }
}

fun Rotations.copy(x: Float? = null, y: Float? = null, z: Float? = null) =
    Rotations(x ?: this.x, y ?: this.y, z ?: this.z)

fun Rotations.add(x: Float, y: Float, z: Float) =
    Rotations(this.x + x, this.y + y, this.z + z)

val serverTick: Int
    get() = MINECRAFT_SERVER.tickCount

fun <E : Any> NonNullList(list: List<E>, default: E? = null): NonNullList<E> {
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

fun BlockPos.setBlockStateNoUpdate(state: BlockState) {
    val section = chunkSection
    val old = section.getBlockState(this)
    section.setBlockStateSilently(this, state)
    world.serverLevel.sendBlockUpdated(nmsPos, old, state, 3)
}

fun BlockPos.setBlockStateSilently(state: BlockState) {
    chunkSection.setBlockStateSilently(this, state)
}

fun BlockPos.setBlockState(state: BlockState) {
    world.serverLevel.setBlock(nmsPos, state, 11)
}

fun BlockPos.getBlockState(): BlockState {
    return chunkSection.getBlockState(this)
}

val BlockPos.chunkSection: LevelChunkSection
    get() {
        val chunk = world.serverLevel.getChunk(x shr 4, z shr 4)
        return chunk.getSection(chunk.getSectionIndex(y))
    }

fun LevelChunkSection.setBlockStateSilently(pos: BlockPos, state: BlockState) {
    setBlockState(pos.x and 0xF, pos.y and 0xF, pos.z and 0xF, state)
}

fun LevelChunkSection.getBlockState(pos: BlockPos): BlockState {
    return getBlockState(pos.x and 0xF, pos.y and 0xF, pos.z and 0xF)
}

inline fun Level.captureDrops(run: () -> Unit): List<ItemEntity> {
    val captureDrops = ArrayList<ItemEntity>()
    this.captureDrops = captureDrops
    try {
        run.invoke()
        return captureDrops
    } finally {
        @Suppress("NULL_FOR_NONNULL_TYPE") // improperly annotated
        this.captureDrops = null
    }
}

fun <T> Either<T, T>.take(): T {
    return left().orElse(null) ?: right().get()
}

fun PlayerList.broadcast(location: Location, maxDistance: Double, packet: Packet<*>) =
    broadcast(null, location.x, location.y, location.z, maxDistance, location.world!!.serverLevel.dimension(), packet)

fun PlayerList.broadcast(block: Block, maxDistance: Double, packet: Packet<*>) =
    broadcast(null, block.x.toDouble(), block.y.toDouble(), block.z.toDouble(), maxDistance, block.world.serverLevel.dimension(), packet)

fun PlayerList.broadcast(exclude: MojangPlayer?, location: Location, maxDistance: Double, packet: Packet<*>) =
    broadcast(exclude, location.x, location.y, location.z, maxDistance, location.world!!.serverLevel.dimension(), packet)

fun PlayerList.broadcast(exclude: MojangPlayer?, block: Block, maxDistance: Double, packet: Packet<*>) =
    broadcast(exclude, block.x.toDouble(), block.y.toDouble(), block.z.toDouble(), maxDistance, block.world.serverLevel.dimension(), packet)

fun PlayerList.broadcast(exclude: Player?, location: Location, maxDistance: Double, packet: Packet<*>) =
    broadcast(exclude?.serverPlayer, location.x, location.y, location.z, maxDistance, location.world!!.serverLevel.dimension(), packet)

fun PlayerList.broadcast(exclude: Player?, block: Block, maxDistance: Double, packet: Packet<*>) =
    broadcast(exclude?.serverPlayer, block.x.toDouble(), block.y.toDouble(), block.z.toDouble(), maxDistance, block.world.serverLevel.dimension(), packet)

operator fun <T> Registry<T>.get(key: String): Optional<Holder.Reference<T>> {
    val id = ResourceLocation.tryParse(key) ?: return Optional.empty()
    return get(id)
}

fun <T> Registry<T>.get(id: ResourceLocation): Holder<T>? {
    val key = ResourceKey.create(key(), id)
    return get(key).getOrNull()
}

fun <T> Registry<T>.getOrNull(key: String): Holder.Reference<T>? {
    return get(key).getOrNull()
}

fun <T> Registry<T>.getOrNull(id: ResourceLocation): Holder.Reference<T>? {
    return get(id).getOrNull()
}

fun <T> Registry<T>.getOrNull(key: Key): Holder.Reference<T>? {
    return getOrNull(ResourceLocation.fromNamespaceAndPath(key.namespace(), key.value()))
}

fun <T> Registry<T>.getOrThrow(key: String): Holder<T> {
    return getOrThrow(ResourceLocation.parse(key))
}

fun <T> Registry<T>.getOrThrow(id: ResourceLocation): Holder<T> {
    val key = ResourceKey.create(key(), id)
    return getOrThrow(key)
}

fun <T> Registry<T>.getOrThrow(key: Key): Holder<T> {
    return getOrThrow(ResourceLocation.fromNamespaceAndPath(key.namespace(), key.value()))
}

fun <T> Registry<T>.getValue(key: String): T? {
    return get(key).getOrNull()?.value()
}

fun <T> Registry<T>.getValueOrThrow(key: String): T {
    return getValueOrThrow(ResourceLocation.parse(key))
}

fun <T> Registry<T>.getValueOrThrow(id: ResourceLocation): T {
    return getOrThrow(ResourceKey.create(key(), id)).value()
}

fun <T> Registry<T>.getOrCreateHolder(id: ResourceLocation): Holder<T> {
    val key = ResourceKey.create(key(), id)
    val holder = get(key)
    
    if (holder.isPresent)
        return holder.get()
    
    if (this !is MappedRegistry<T>)
        throw IllegalStateException("Can't create holder for non MappedRegistry ${this.key()}")
    
    return this.createRegistrationLookup().getOrThrow(key)
}

operator fun Registry<*>.contains(key: String): Boolean {
    val id = ResourceLocation.tryParse(key) ?: return false
    return containsKey(id)
}

operator fun <T : Any> WritableRegistry<T>.set(name: String, value: T) {
    register(ResourceKey.create(key(), ResourceLocation.parse(name)), value, RegistrationInfo.BUILT_IN)
}

operator fun <T : Any> WritableRegistry<T>.set(id: ResourceLocation, value: T) {
    register(ResourceKey.create(key(), id), value, RegistrationInfo.BUILT_IN)
}

operator fun <T : Any> WritableRegistry<T>.set(key: ResourceKey<T>, value: T) {
    register(key, value, RegistrationInfo.BUILT_IN)
}

operator fun <T : Any> WritableRegistry<T>.set(addon: Addon, key: String, value: T) {
    register(ResourceKey.create(key(), ResourceLocation(addon, key)), value, RegistrationInfo.BUILT_IN)
}

fun <T : Any> WritableRegistry<T>.register(id: ResourceLocation, value: T): Holder.Reference<T> {
    return register(ResourceKey.create(key(), id), value, RegistrationInfo.BUILT_IN)
}

fun <T> Registry<T>.toHolderMap(): Map<ResourceLocation, Holder<T>> {
    val map = HashMap<ResourceLocation, Holder<T>>()
    for (key in registryKeySet()) {
        val holderOptional = get(key)
        if (holderOptional.isEmpty)
            continue
        
        map[key.location()] = holderOptional.get()
    }
    
    return map
}

fun <T> Registry<T>.toMap(): Map<ResourceLocation, T> {
    val map = HashMap<ResourceLocation, T>()
    for (key in registryKeySet()) {
        val holderOptional = get(key)
        if (holderOptional.isEmpty)
            continue
        
        val holder = holderOptional.get()
        if (!holder.isBound)
            continue
        
        map[key.location()] = holder.value()
    }
    
    return map
}

operator fun <T> ResourceKey<Registry<T>>.get(key: ResourceKey<T>): Holder.Reference<T>? {
    return REGISTRY_ACCESS.get(key).getOrNull()
}

operator fun <T> ResourceKey<Registry<T>>.get(id: ResourceLocation): Holder.Reference<T>? {
    return get(ResourceKey.create<T>(this, id))
}

operator fun <T> ResourceKey<Registry<T>>.get(id: String): Holder.Reference<T>? {
    return get(ResourceLocation.parse(id))
}

fun <T> ResourceKey<Registry<T>>.getOrThrow(key: ResourceKey<T>): Holder.Reference<T> {
    return REGISTRY_ACCESS.get(key).get()
}

fun <T> ResourceKey<Registry<T>>.getOrThrow(id: ResourceLocation): Holder.Reference<T> {
    return REGISTRY_ACCESS.get(ResourceKey.create<T>(this, id)).get()
}

fun <T> ResourceKey<Registry<T>>.getOrThrow(key: String): Holder.Reference<T> {
    return getOrThrow(ResourceLocation.parse(key))
}

fun <T> ResourceKey<Registry<T>>.getValue(id: ResourceLocation): T? {
    return get(id)?.value()
}

fun <T> ResourceKey<Registry<T>>.getValue(key: String): T? {
    return get(key)?.value()
}

fun <T : Any> RegistryAccess.getOrThrow(key: ResourceKey<T>): Holder.Reference<T> {
    return REGISTRY_ACCESS.get(key).get()
}

fun <T : Any> RegistryAccess.getValue(key: ResourceKey<T>): T? {
    return REGISTRY_ACCESS.get(key).getOrNull()?.value()
}

fun <T : Any> RegistryAccess.getValueOrThrow(key: ResourceKey<T>): T {
    return REGISTRY_ACCESS.get(key).get().value()
}

fun <T> RegistryInfoLookup.lookupGetterOrThrow(key: ResourceKey<Registry<T>>): HolderGetter<T> {
    return lookup(key).getOrNull()?.getter ?: throw IllegalArgumentException("Registry not found: $key")
}

fun ResourceLocation.toString(separator: String): String {
    return namespace + separator + path
}

fun ResourceLocation(addon: Addon, name: String): ResourceLocation {
    return ResourceLocation.fromNamespaceAndPath(addon.id, name)
}

fun preventPacketBroadcast(run: () -> Unit) {
    BroadcastPacketPatch.dropAll = true
    try {
        run.invoke()
    } finally {
        BroadcastPacketPatch.dropAll = false
    }
}

fun replaceBroadcastExclusion(exclude: ServerPlayer, run: () -> Unit) {
    BroadcastPacketPatch.exclude = exclude
    try {
        run.invoke()
    } finally {
        BroadcastPacketPatch.exclude = null
    }
}

fun forcePacketBroadcast(run: () -> Unit) {
    BroadcastPacketPatch.ignoreExcludedPlayer = true
    try {
        run.invoke()
    } finally {
        BroadcastPacketPatch.ignoreExcludedPlayer = false
    }
}

internal inline fun withoutBlockMigration(pos: BlockPos, run: () -> Unit) {
    val chunkSection = pos.chunkSection as LevelChunkSectionWrapper
    val previous = chunkSection.isMigrationActive
    chunkSection.isMigrationActive = false
    try {
        run.invoke()
    } finally {
        chunkSection.isMigrationActive = previous
    }
}

fun RegistryFriendlyByteBuf(): RegistryFriendlyByteBuf =
    RegistryFriendlyByteBuf(Unpooled.buffer(), REGISTRY_ACCESS)

object NMSUtils {
    
    val ENTITY_COUNTER by lazy {
        ReflectionUtils.getField(
            MojangEntity::class.java,
            true,
            "ENTITY_COUNTER"
        ).get(null) as AtomicInteger
    }
    
}
