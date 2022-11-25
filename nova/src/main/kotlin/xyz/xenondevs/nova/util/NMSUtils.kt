@file:Suppress("unused")

package xyz.xenondevs.nova.util

import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.core.Rotations
import net.minecraft.network.protocol.Packet
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.dedicated.DedicatedServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.craftbukkit.v1_19_R1.CraftServer
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_19_R1.util.CraftMagicNumbers
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.transformer.patch.playerlist.BroadcastPacketPatch
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.world.BlockPos
import java.util.concurrent.atomic.AtomicInteger
import net.minecraft.core.BlockPos as MojangBlockPos
import net.minecraft.world.entity.Entity as MojangEntity
import net.minecraft.world.entity.EquipmentSlot as MojangEquipmentSlot
import net.minecraft.world.item.ItemStack as MojangStack
import net.minecraft.world.level.block.Block as MojangBlock

val Entity.nmsEntity: MojangEntity
    get() = (this as CraftEntity).handle

val Player.serverPlayer: ServerPlayer
    get() = (this as CraftPlayer).handle

@Deprecated("Misleading name", replaceWith = ReplaceWith("nmsCopy"))
val ItemStack.nmsStack: MojangStack
    get() = CraftItemStack.asNMSCopy(this)

val ItemStack?.nmsCopy: MojangStack
    get() = CraftItemStack.asNMSCopy(this)

@Deprecated("Misleading name", replaceWith = ReplaceWith("bukkitCopy"))
val MojangStack.bukkitStack: ItemStack
    get() = CraftItemStack.asBukkitCopy(this)

val MojangStack.bukkitCopy: ItemStack
    get() = CraftItemStack.asBukkitCopy(this)

val MojangStack.bukkitMirror: ItemStack
    get() = CraftItemStack.asCraftMirror(this)

val Location.blockPos: MojangBlockPos
    get() = MojangBlockPos(blockX, blockY, blockZ)

val BlockPos.nmsPos: MojangBlockPos
    get() = MojangBlockPos(x, y, z)

val MojangBlockPos.vec3: Vec3
    get() = Vec3(x.toDouble(), y.toDouble(), z.toDouble())

val MojangBlockPos.center: Vec3
    get() = Vec3(x + 0.5, y + 0.5, z + 0.5)

val World.serverLevel: ServerLevel
    get() = (this as CraftWorld).handle

val Player.connection: ServerGamePacketListenerImpl
    get() = serverPlayer.connection

val NamespacedKey.resourceLocation: ResourceLocation
    get() = ResourceLocation(toString())

@Suppress("DEPRECATION")
val ResourceLocation.namespacedKey: NamespacedKey
    get() = NamespacedKey(namespace, path)

val InteractionHand.bukkitSlot: EquipmentSlot
    get() = when (this) {
        InteractionHand.MAIN_HAND -> EquipmentSlot.HAND
        InteractionHand.OFF_HAND -> EquipmentSlot.OFF_HAND
    }

val EquipmentSlot.interactionHand: InteractionHand
    get() = when (this) {
        EquipmentSlot.HAND -> InteractionHand.MAIN_HAND
        EquipmentSlot.OFF_HAND -> InteractionHand.OFF_HAND
        else -> throw UnsupportedOperationException()
    }

val EquipmentSlot.nmsEquipmentSlot: MojangEquipmentSlot
    get() = when (this) {
        EquipmentSlot.HAND -> MojangEquipmentSlot.MAINHAND
        EquipmentSlot.OFF_HAND -> MojangEquipmentSlot.OFFHAND
        EquipmentSlot.FEET -> MojangEquipmentSlot.FEET
        EquipmentSlot.LEGS -> MojangEquipmentSlot.LEGS
        EquipmentSlot.CHEST -> MojangEquipmentSlot.CHEST
        EquipmentSlot.HEAD -> MojangEquipmentSlot.HEAD
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

val Material.nmsBlock: MojangBlock
    get() = CraftMagicNumbers.getBlock(this)

val Block.nmsState: BlockState
    get() = world.serverLevel.getBlockState(MojangBlockPos(x, y, z))

fun MojangBlockPos.toNovaPos(world: World): BlockPos =
    BlockPos(world, x, y, z)

fun Player.send(vararg packets: Packet<*>) {
    val connection = connection
    packets.forEach { connection.send(it) }
}

fun Rotations.copy(x: Float? = null, y: Float? = null, z: Float? = null) =
    Rotations(x ?: this.x, y ?: this.y, z ?: this.z)

fun Rotations.add(x: Float, y: Float, z: Float) =
    Rotations(this.x + x, this.y + y, this.z + z)

val minecraftServer: DedicatedServer = (Bukkit.getServer() as CraftServer).server

val serverTick: Int
    get() = minecraftServer.tickCount

@Suppress("FunctionName")
fun <E> NonNullList(list: List<E>, default: E? = null): NonNullList<E> {
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
        this.captureDrops = null
    }
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

object NMSUtils {
    
    val ENTITY_COUNTER = ReflectionUtils.getField(
        MojangEntity::class.java,
        true,
        "SRF(net.minecraft.world.entity.Entity ENTITY_COUNTER)"
    ).get(null) as AtomicInteger
    
}
