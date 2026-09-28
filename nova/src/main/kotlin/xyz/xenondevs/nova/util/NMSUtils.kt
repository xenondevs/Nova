@file:Suppress("unused")

package xyz.xenondevs.nova.util

import com.mojang.datafixers.util.Either
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.set.RegistryKeySet
import io.papermc.paper.registry.tag.Tag
import net.kyori.adventure.key.Key
import net.kyori.adventure.key.Namespaced
import net.minecraft.core.DefaultedRegistry
import net.minecraft.core.Direction
import net.minecraft.core.Holder
import net.minecraft.core.HolderGetter
import net.minecraft.core.HolderSet
import net.minecraft.core.MappedRegistry
import net.minecraft.core.NonNullList
import net.minecraft.core.RegistrationInfo
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.core.Rotations
import net.minecraft.core.WritableRegistry
import net.minecraft.network.protocol.Packet
import net.minecraft.resources.Identifier
import net.minecraft.resources.RegistryOps.RegistryInfoLookup
import net.minecraft.resources.ResourceKey
import net.minecraft.server.dedicated.DedicatedServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.players.PlayerList
import net.minecraft.tags.TagKey
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.ItemInstance
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockType
import org.bukkit.block.PistonMoveReaction
import org.bukkit.block.data.BlockData
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.block.CraftBlockType
import org.bukkit.craftbukkit.block.data.CraftBlockData
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftFallingBlock
import org.bukkit.craftbukkit.entity.CraftLivingEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.craftbukkit.inventory.CraftItemType
import org.bukkit.craftbukkit.util.CraftMagicNumbers
import org.bukkit.entity.Entity
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Pose
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import org.bukkit.util.Vector
import org.joml.Vector3d
import xyz.xenondevs.invui.util.ColorPalette
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.world.block.NoteBlockInstrument
import java.util.*
import kotlin.jvm.optionals.getOrNull
import net.minecraft.core.BlockPos as MojangBlockPos
import net.minecraft.world.entity.Entity as MojangEntity
import net.minecraft.world.entity.EquipmentSlot as MojangEquipmentSlot
import net.minecraft.world.entity.LivingEntity as MojangLivingEntity
import net.minecraft.world.entity.Pose as MojangPose
import net.minecraft.world.entity.ai.attributes.Attribute as MojangAttribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier as MojangAttributeModifier
import net.minecraft.world.entity.item.FallingBlockEntity as MojangFallingBlockEntity
import net.minecraft.world.entity.player.Player as MojangPlayer
import net.minecraft.world.item.Item as MojangItem
import net.minecraft.world.item.ItemStack as MojangStack
import net.minecraft.world.item.ItemUseAnimation as MojangItemUseAnimation
import net.minecraft.world.level.block.Block as MojangBlock
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument as MojangNoteBlockInstrument

val MINECRAFT_SERVER: DedicatedServer by lazy { (Bukkit.getServer() as CraftServer).server }
val REGISTRY_ACCESS: RegistryAccess by lazy { MINECRAFT_SERVER.registryAccess() }
val DATA_VERSION: Int by lazy { CraftMagicNumbers.INSTANCE.dataVersion }

val Entity.nmsEntity: MojangEntity
    get() = (this as CraftEntity).handle

val LivingEntity.nmsEntity: MojangLivingEntity
    get() = (this as CraftLivingEntity).handle

val Player.serverPlayer: ServerPlayer
    get() = (this as CraftPlayer).handle

val FallingBlock.nmsEntity: MojangFallingBlockEntity
    get() = (this as CraftFallingBlock).handle

fun ItemStack?.unwrap(): MojangStack =
    this?.let(CraftItemStack::unwrap) ?: MojangStack.EMPTY

fun MojangStack.asBukkitMirror(): ItemStack =
    CraftItemStack.asBukkitMirror(this)

fun ItemInstance.asBukkitCopy(): ItemStack =
    CraftItemStack.asBukkitCopy(this)

val BlockData.nmsBlockState: BlockState
    get() = (this as CraftBlockData).state

val BlockState.bukkitBlockData: BlockData
    get() = asBlockData()

internal fun BlockState.toPropertyStringMap(): Map<String, String> =
    properties.associate { serializeProperty(this, it) }

private fun <T : Comparable<T>> serializeProperty(state: BlockState, property: Property<T>): Pair<String, String> =
    property.name to property.getName(state.getValue(property))

internal fun Color?.toNmsMapColor(): MapColor {
    if (this == null)
        return MapColor.NONE
    val packedColor = ColorPalette.getColor(this).toInt() and 0xFF
    return MapColor.byId(packedColor shr 2)
}

val Location.Block: MojangBlockPos
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

val NamespacedKey.identifier: Identifier
    get() = Identifier.fromNamespaceAndPath(namespace, key)

val Identifier.namespacedKey: NamespacedKey
    get() = NamespacedKey(namespace, path)

fun <C : ResourceType> Identifier.toResourcePath(type: C): ResourcePath<C> =
    ResourcePath(type, namespace, path)

fun Key.toIdentifier(): Identifier =
    Identifier.fromNamespaceAndPath(namespace(), value())

fun Identifier.toKey(): Key =
    Key.key(namespace, path)

fun Identifier.toNamespacedKey(): NamespacedKey =
    NamespacedKey(namespace, path)

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
        EquipmentSlot.SADDLE -> MojangEquipmentSlot.SADDLE
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
        MojangEquipmentSlot.SADDLE -> EquipmentSlot.SADDLE
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

val ItemUseAnimation.nmsItemUseAnimation: MojangItemUseAnimation
    get() = when (this) {
        ItemUseAnimation.NONE -> MojangItemUseAnimation.NONE
        ItemUseAnimation.EAT -> MojangItemUseAnimation.EAT
        ItemUseAnimation.DRINK -> MojangItemUseAnimation.DRINK
        ItemUseAnimation.BLOCK -> MojangItemUseAnimation.BLOCK
        ItemUseAnimation.BOW -> MojangItemUseAnimation.BOW
        ItemUseAnimation.SPEAR -> MojangItemUseAnimation.SPEAR
        ItemUseAnimation.CROSSBOW -> MojangItemUseAnimation.CROSSBOW
        ItemUseAnimation.SPYGLASS -> MojangItemUseAnimation.SPYGLASS
        ItemUseAnimation.TOOT_HORN -> MojangItemUseAnimation.TOOT_HORN
        ItemUseAnimation.BRUSH -> MojangItemUseAnimation.BRUSH
        ItemUseAnimation.BUNDLE -> MojangItemUseAnimation.BUNDLE
        ItemUseAnimation.TRIDENT -> MojangItemUseAnimation.TRIDENT
    }

val PistonMoveReaction.nmsPushReaction: PushReaction
    get() = when (this) {
        PistonMoveReaction.MOVE -> PushReaction.PUSH_PULL
        PistonMoveReaction.BREAK -> PushReaction.POPPED
        PistonMoveReaction.BLOCK -> PushReaction.IMMOVEABLE
        PistonMoveReaction.IGNORE -> PushReaction.IGNORE_ENTITY
        PistonMoveReaction.PUSH_ONLY -> PushReaction.PUSH
    }

internal val NoteBlockInstrument.nmsNoteBlockInstrument: MojangNoteBlockInstrument
    get() = when (this) {
        NoteBlockInstrument.HARP -> MojangNoteBlockInstrument.HARP
        NoteBlockInstrument.BASS_DRUM -> MojangNoteBlockInstrument.BASEDRUM
        NoteBlockInstrument.SNARE_DRUM -> MojangNoteBlockInstrument.SNARE
        NoteBlockInstrument.CLICKS_AND_STICKS -> MojangNoteBlockInstrument.HAT
        NoteBlockInstrument.BASS_GUITAR -> MojangNoteBlockInstrument.BASS
        NoteBlockInstrument.FLUTE -> MojangNoteBlockInstrument.FLUTE
        NoteBlockInstrument.BELL -> MojangNoteBlockInstrument.BELL
        NoteBlockInstrument.GUITAR -> MojangNoteBlockInstrument.GUITAR
        NoteBlockInstrument.CHIME -> MojangNoteBlockInstrument.CHIME
        NoteBlockInstrument.XYLOPHONE -> MojangNoteBlockInstrument.XYLOPHONE
        NoteBlockInstrument.IRON_XYLOPHONE -> MojangNoteBlockInstrument.IRON_XYLOPHONE
        NoteBlockInstrument.COW_BELL -> MojangNoteBlockInstrument.COW_BELL
        NoteBlockInstrument.DIDGERIDOO -> MojangNoteBlockInstrument.DIDGERIDOO
        NoteBlockInstrument.BIT -> MojangNoteBlockInstrument.BIT
        NoteBlockInstrument.BANJO -> MojangNoteBlockInstrument.BANJO
        NoteBlockInstrument.PLING -> MojangNoteBlockInstrument.PLING
        NoteBlockInstrument.TRUMPET -> MojangNoteBlockInstrument.TRUMPET
        NoteBlockInstrument.EXPOSED_TRUMPET -> MojangNoteBlockInstrument.TRUMPET_EXPOSED
        NoteBlockInstrument.WEATHERED_TRUMPET -> MojangNoteBlockInstrument.TRUMPET_WEATHERED
        NoteBlockInstrument.OXIDIZED_TRUMPET -> MojangNoteBlockInstrument.TRUMPET_OXIDIZED
        NoteBlockInstrument.ZOMBIE -> MojangNoteBlockInstrument.ZOMBIE
        NoteBlockInstrument.SKELETON -> MojangNoteBlockInstrument.SKELETON
        NoteBlockInstrument.CREEPER -> MojangNoteBlockInstrument.CREEPER
        NoteBlockInstrument.DRAGON -> MojangNoteBlockInstrument.DRAGON
        NoteBlockInstrument.WITHER_SKELETON -> MojangNoteBlockInstrument.WITHER_SKELETON
        NoteBlockInstrument.PIGLIN -> MojangNoteBlockInstrument.PIGLIN
    }

val BlockType.nmsBlock: MojangBlock
    get() = (this as CraftBlockType<*>).handle

val ItemType.nmsItem: MojangItem
    get() = (this as CraftItemType<*>).handle

val MojangItem.bukkitItemType: ItemType
    get() = CraftItemType.minecraftToBukkitNew(this)

val Block.nmsPos: MojangBlockPos
    get() = MojangBlockPos(x, y, z)

@Deprecated("", ReplaceWith("nmsBlockState"))
val Block.nmsState: BlockState
    get() = nmsBlockState

val Block.nmsBlockState: BlockState
    get() = world.serverLevel.getBlockState(nmsPos)

val Block.nmsBlockEntity: BlockEntity?
    get() = world.serverLevel.getBlockEntity(nmsPos)

val BlockState.id: Int
    get() = MojangBlock.getId(this)

fun MojangBlockPos.toBlock(world: World): Block =
    world.getBlockAt(x, y, z)

fun Vec3.toVector3d(): Vector3d =
    Vector3d(x, y, z)

fun ItemStack.toNmsTemplate(): ItemStackTemplate? =
    unwrap().toTemplate()

fun MojangStack.toTemplate(): ItemStackTemplate? =
    if (!isEmpty) ItemStackTemplate.fromNonEmptyStack(this) else null

fun Rotations.with(x: Float? = null, y: Float? = null, z: Float? = null) =
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
    return hasProperty(property) && getValue(property) == value
}

fun Block.setBlockStateNoUpdate(state: BlockState) {
    val section = chunkSection
    val old = section.getBlockState(this)
    section.setBlockStateSilently(this, state)
    world.serverLevel.sendBlockUpdated(nmsPos, old, state, 3)
}

fun Block.setBlockStateSilently(state: BlockState) {
    chunkSection.setBlockStateSilently(this, state)
}

fun Block.setBlockState(state: BlockState) {
    world.serverLevel.setBlock(nmsPos, state, 11)
}

fun Block.getBlockState(): BlockState {
    return chunkSection.getBlockState(this)
}

val Block.chunkSection: LevelChunkSection
    get() {
        val chunk = world.serverLevel.getChunk(x shr 4, z shr 4)
        return chunk.getSection(chunk.getSectionIndex(y))
    }

fun LevelChunkSection.setBlockStateSilently(block: Block, state: BlockState) {
    setBlockState(block.x and 0xF, block.y and 0xF, block.z and 0xF, state)
}

fun LevelChunkSection.getBlockState(block: Block): BlockState {
    return getBlockState(block.x and 0xF, block.y and 0xF, block.z and 0xF)
}

inline fun Level.captureDrops(run: () -> Unit): List<ItemEntity> {
    val captureDrops = ArrayList<ItemEntity>()
    this.captureDrops = captureDrops
    try {
        run.invoke()
        return captureDrops
    } finally {
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

operator fun <T : Any> Registry<T>.get(key: String): Optional<Holder.Reference<T>> {
    val id = Identifier.tryParse(key) ?: return Optional.empty()
    return get(id)
}

fun <T : Any> Registry<T>.getOrNull(key: String): Holder.Reference<T>? {
    return get(key).getOrNull()
}

fun <T : Any> Registry<T>.getOrNull(id: Identifier): Holder.Reference<T>? {
    return get(id).getOrNull()
}

fun <T : Any> Registry<T>.getOrNull(key: Key): Holder.Reference<T>? {
    return getOrNull(Identifier.fromNamespaceAndPath(key.namespace(), key.value()))
}

fun <T : Any> Registry<T>.getOrThrow(key: String): Holder<T> {
    return getOrThrow(Identifier.parse(key))
}

fun <T : Any> Registry<T>.getOrThrow(id: Identifier): Holder<T> {
    val key = ResourceKey.create(key(), id)
    return getOrThrow(key)
}

fun <T : Any> Registry<T>.getOrThrow(key: Key): Holder<T> {
    return getOrThrow(Identifier.fromNamespaceAndPath(key.namespace(), key.value()))
}

fun <T : Any> Registry<T>.getValue(key: String?): T? {
    return getValue(key?.let(Identifier::parse))
}

fun <T : Any> Registry<T>.getValue(key: Key?): T? {
    return getValue(key?.toIdentifier())
}

fun <T : Any> DefaultedRegistry<T>.getValue(key: String?): T {
    return getValue(key?.let(Identifier::parse))
}

fun <T : Any> DefaultedRegistry<T>.getValue(key: Key?): T {
    return getValue(key?.toIdentifier())
}

fun <T : Any> Registry<T>.getValueOrThrow(key: String): T {
    return getValueOrThrow(Identifier.parse(key))
}

fun <T : Any> Registry<T>.getValueOrThrow(key: Key): T {
    return getValueOrThrow(key.toIdentifier())
}

fun <T : Any> Registry<T>.getValueOrThrow(id: Identifier): T {
    return getOrThrow(ResourceKey.create(key(), id)).value()
}

fun <T : Any> Registry<T>.getValueOrNull(key: String): T? {
    return Identifier.tryParse(key)?.let(::getValueOrThrow)
}

fun <T : Any> Registry<T>.getValueOrNull(key: Key): T? {
    return getValueOrNull(key.toIdentifier())
}

fun <T : Any> Registry<T>.getValueOrNull(id: Identifier): T? {
    return getOrNull(id)?.takeIf { it.isBound }?.value()
}

fun <T : Any> Registry<T>.getOrCreateHolder(id: Identifier): Holder<T> {
    val key = ResourceKey.create(key(), id)
    val holder = get(key)
    
    if (holder.isPresent)
        return holder.get()
    
    if (this !is MappedRegistry<T>)
        throw IllegalStateException("Can't create holder for non MappedRegistry ${this.key()}")
    
    return this.createRegistrationLookup().getOrThrow(key)
}

operator fun Registry<*>.contains(key: String): Boolean {
    val id = Identifier.tryParse(key) ?: return false
    return containsKey(id)
}

operator fun Registry<*>.contains(key: Key): Boolean {
    return containsKey(key.toIdentifier())
}

operator fun <T : Any> WritableRegistry<T>.set(name: String, value: T) {
    register(ResourceKey.create(key(), Identifier.parse(name)), value, RegistrationInfo.BUILT_IN)
}

operator fun <T : Any> WritableRegistry<T>.set(id: Identifier, value: T) {
    register(ResourceKey.create(key(), id), value, RegistrationInfo.BUILT_IN)
}

operator fun <T : Any> WritableRegistry<T>.set(key: ResourceKey<T>, value: T) {
    register(key, value, RegistrationInfo.BUILT_IN)
}

operator fun <T : Any> WritableRegistry<T>.set(addon: Addon, key: String, value: T) {
    register(ResourceKey.create(key(), Identifier(addon, key)), value, RegistrationInfo.BUILT_IN)
}

operator fun <T : Any> WritableRegistry<T>.set(key: Key, value: T) {
    register(key.toIdentifier(), value)
}

fun <T : Any> WritableRegistry<T>.register(id: Identifier, value: T): Holder.Reference<T> {
    return register(ResourceKey.create(key(), id), value, RegistrationInfo.BUILT_IN)
}

fun <T : Any> WritableRegistry<T>.register(id: Key, value: T): Holder.Reference<T> {
    return register(id.toIdentifier(), value)
}

fun <T : Any> Registry<T>.toHolderMap(): Map<Identifier, Holder<T>> {
    val map = HashMap<Identifier, Holder<T>>()
    for (key in registryKeySet()) {
        val holderOptional = get(key)
        if (holderOptional.isEmpty)
            continue
        
        map[key.identifier()] = holderOptional.get()
    }
    
    return map
}

fun <T : Any> Registry<T>.toMap(): Map<Identifier, T> {
    val map = HashMap<Identifier, T>()
    for (key in registryKeySet()) {
        val holderOptional = get(key)
        if (holderOptional.isEmpty)
            continue
        
        val holder = holderOptional.get()
        if (!holder.isBound)
            continue
        
        map[key.identifier()] = holder.value()
    }
    
    return map
}

operator fun <T : Any> ResourceKey<Registry<T>>.get(key: ResourceKey<T>): Holder.Reference<T>? {
    return REGISTRY_ACCESS.get(key).getOrNull()
}

operator fun <T : Any> ResourceKey<Registry<T>>.get(id: Identifier): Holder.Reference<T>? {
    return get(ResourceKey.create(this, id))
}

operator fun <T : Any> ResourceKey<Registry<T>>.get(id: String): Holder.Reference<T>? {
    return get(Identifier.parse(id))
}

fun <T : Any> ResourceKey<Registry<T>>.getOrThrow(key: ResourceKey<T>): Holder.Reference<T> {
    return REGISTRY_ACCESS.get(key).get()
}

fun <T : Any> ResourceKey<Registry<T>>.getOrThrow(id: Identifier): Holder.Reference<T> {
    return REGISTRY_ACCESS.get(ResourceKey.create<T>(this, id)).get()
}

fun <T : Any> ResourceKey<Registry<T>>.getOrThrow(id: Key): Holder.Reference<T> {
    return getOrThrow(id.toIdentifier())
}

fun <T : Any> ResourceKey<Registry<T>>.getOrThrow(key: String): Holder.Reference<T> {
    return getOrThrow(Identifier.parse(key))
}

fun <T : Any> ResourceKey<Registry<T>>.getValue(id: Identifier): T? {
    return get(id)?.value()
}

fun <T : Any> ResourceKey<Registry<T>>.getValue(key: String): T? {
    return get(key)?.value()
}

fun <T : Any> RegistryAccess.getOrThrow(key: ResourceKey<T>): Holder.Reference<T> {
    return get(key).get()
}

fun <T : Any> RegistryAccess.getValue(key: ResourceKey<T>): T? {
    return get(key).getOrNull()?.value()
}

fun <T : Any> RegistryAccess.getValueOrThrow(key: ResourceKey<T>): T {
    return get(key).get().value()
}

fun <T : Any> RegistryInfoLookup.lookupGetterOrThrow(key: ResourceKey<Registry<T>>): HolderGetter<T> {
    return lookup(key).getOrNull() ?: throw IllegalArgumentException("Registry not found: $key")
}

fun Identifier.toString(separator: String): String {
    return namespace + separator + path
}

fun Identifier(namespaced: Namespaced, name: String): Identifier {
    return parseKey(name, namespaced).toIdentifier()
}

fun <T : Any> io.papermc.paper.registry.tag.TagKey<*>.toNmsTagKey(): TagKey<T> =
    TagKey.create(registryKey().toResourceKey(), key().toIdentifier())

fun <T : Any> RegistryKey<*>.toResourceKey(): ResourceKey<Registry<T>> =
    ResourceKey.createRegistryKey(key().toIdentifier())

fun <T : Any> TypedKey<*>.toResourceKey(): ResourceKey<T> =
    ResourceKey.create(registryKey().toResourceKey(), key().toIdentifier())

fun <T : Any> Iterable<TypedKey<*>>.toHolderSet(registry: HolderGetter<T>): HolderSet<T> =
    HolderSet.direct(map { registry.getOrThrow(it.toResourceKey()) })

fun <T : Any> RegistryKeySet<*>.toHolderSet(registry: HolderGetter<T>): HolderSet<T> {
    return when (this) {
        is Tag -> registry.getOrThrow(tagKey().toNmsTagKey())
        else -> values().toHolderSet(registry)
    }
}

fun <T : Any> RegistryEntry.Paper<*>.toHolder(registry: HolderGetter<T>): Holder.Reference<T> =
    registry.getOrThrow(ResourceKey.create(this.registry.toResourceKey(), key.toIdentifier()))

fun <T : Any> RegistryEntrySet.Paper<*>.toHolderSet(registry: HolderGetter<T>): HolderSet<T> {
    return when (this) {
        is RegistryEntrySet.Paper.Tag<*> -> registry.getOrThrow(tagKey.toNmsTagKey())
        is RegistryEntrySet.Paper.Direct<*> -> HolderSet.direct(entries.map {
            registry.getOrThrow(ResourceKey.create(this.registry.toResourceKey(), it.key.toIdentifier()))
        })
    }
}

fun preventPacketBroadcast(run: () -> Unit) {
    NMSUtils.broadcastDropAll.set(true)
    try {
        run.invoke()
    } finally {
        NMSUtils.broadcastDropAll.set(false)
    }
}

fun replaceBroadcastExclusion(exclude: ServerPlayer, run: () -> Unit) {
    NMSUtils.broadcastExcludedPlayerOverride.set(exclude)
    try {
        run.invoke()
    } finally {
        NMSUtils.broadcastExcludedPlayerOverride.set(null)
    }
}

fun forcePacketBroadcast(run: () -> Unit) {
    NMSUtils.broadcastIgnoreExcludedPlayer.set(true)
    try {
        run.invoke()
    } finally {
        NMSUtils.broadcastIgnoreExcludedPlayer.set(false)
    }
}

@PublishedApi
internal object NMSUtils {
    
    @JvmField
    val broadcastIgnoreExcludedPlayer: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
    
    @JvmField
    val broadcastExcludedPlayerOverride: ThreadLocal<ServerPlayer?> = ThreadLocal.withInitial { null }
    
    @JvmField
    val broadcastDropAll: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
    
}
