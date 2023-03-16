@file:Suppress("unused")

package xyz.xenondevs.nmsutils.particle

import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.DustColorTransitionOptions
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ItemParticleOption
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.particles.SculkChargeParticleOptions
import net.minecraft.core.particles.ShriekParticleOption
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.core.particles.VibrationParticleOption
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.world.item.Item
import net.minecraft.world.level.gameevent.BlockPositionSource
import net.minecraft.world.level.gameevent.EntityPositionSource
import net.minecraft.world.phys.Vec3
import org.bukkit.Axis
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_19_R3.util.CraftMagicNumbers
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import org.joml.Vector3f
import xyz.xenondevs.nmsutils.internal.util.blockPos
import xyz.xenondevs.nmsutils.internal.util.nmsBlock
import xyz.xenondevs.nmsutils.internal.util.nmsEntity
import xyz.xenondevs.nmsutils.internal.util.nmsStack
import xyz.xenondevs.nmsutils.internal.util.send
import java.awt.Color
import java.util.function.Predicate
import net.minecraft.world.item.ItemStack as MojangStack
import net.minecraft.world.level.block.Block as MojangBlock
import net.minecraft.world.level.block.state.BlockState as MojangBlockState
import org.bukkit.Color as BukkitColor

class ParticleBuilder<T : ParticleOptions>(private val particle: ParticleType<T>) {
    
    private lateinit var options: T
    private lateinit var location: Location
    private var longDistance: Boolean = true
    private var offsetX: Float = 0f
    private var offsetY: Float = 0f
    private var offsetZ: Float = 0f
    private var speed: Float = 1f
    private var amount: Int = 0
    
    constructor(particle: ParticleType<T>, location: Location) : this(particle) {
        this.location = location
    }
    
    init {
        @Suppress("UNCHECKED_CAST") // T is always SimpleParticleType
        if (particle is SimpleParticleType)
            options = particle as T
    }
    
    fun location(location: Location) = apply { this.location = location }
    
    fun longDistance(longDistance: Boolean) = apply { this.longDistance = longDistance }
    
    fun offsetX(offsetX: Float) = apply { this.offsetX = offsetX }
    
    fun offsetX(offsetX: Double) = apply { this.offsetX = offsetX.toFloat() }
    
    fun offsetY(offsetY: Float) = apply { this.offsetY = offsetY }
    
    fun offsetY(offsetY: Double) = apply { this.offsetY = offsetY.toFloat() }
    
    fun offsetZ(offsetZ: Float) = apply { this.offsetZ = offsetZ }
    
    fun offsetZ(offsetZ: Double) = apply { this.offsetZ = offsetZ.toFloat() }
    
    fun offset(offsetX: Float, offsetY: Float, offsetZ: Float) = apply {
        this.offsetX = offsetX
        this.offsetY = offsetY
        this.offsetZ = offsetZ
    }
    
    fun offset(offsetX: Double, offsetY: Double, offsetZ: Double) = apply {
        this.offsetX = offsetX.toFloat()
        this.offsetY = offsetY.toFloat()
        this.offsetZ = offsetZ.toFloat()
    }
    
    fun offset(offset: Vector) = apply {
        this.offsetX = offset.x.toFloat()
        this.offsetY = offset.y.toFloat()
        this.offsetZ = offset.z.toFloat()
    }
    
    fun offset(offset: Vec3) = apply {
        this.offsetX = offset.x.toFloat()
        this.offsetY = offset.y.toFloat()
        this.offsetZ = offset.z.toFloat()
    }
    
    fun offset(offset: Vector3f) = apply {
        this.offsetX = offset.x()
        this.offsetY = offset.y()
        this.offsetZ = offset.z()
    }
    
    fun offset(axis: Axis, offset: Float) = apply {
        when (axis) {
            Axis.X -> offsetX = offset
            Axis.Y -> offsetY = offset
            Axis.Z -> offsetZ = offset
        }
    }
    
    fun offset(axis: Axis, offset: Double) = apply {
        when (axis) {
            Axis.X -> offsetX = offset.toFloat()
            Axis.Y -> offsetY = offset.toFloat()
            Axis.Z -> offsetZ = offset.toFloat()
        }
    }
    
    fun offset(color: Color) = apply {
        this.offsetX = color.red / 255f
        this.offsetY = color.green / 255f
        this.offsetZ = color.blue / 255f
    }
    
    fun offset(color: BukkitColor) = apply {
        this.offsetX = color.red / 255f
        this.offsetY = color.green / 255f
        this.offsetZ = color.blue / 255f
    }
    
    fun speed(speed: Float) = apply { this.speed = speed }
    
    fun amount(amount: Int) = apply { this.amount = amount }
    
    fun options(options: T) = apply { this.options = options }
    
    fun options(options: (ParticleType<T>) -> T) = apply { this.options = options(particle) }
    
    fun build() = ClientboundLevelParticlesPacket(
        options,
        longDistance,
        location.x,
        location.y,
        location.z,
        offsetX,
        offsetY,
        offsetZ,
        speed,
        amount
    )
    
    fun display() {
        val packet = build()
        location.world!!.players.forEach { it.send(packet) }
    }
    
    fun display(player: Player) {
        player.send(build())
    }
    
    fun display(vararg players: Player) {
        val packet = build()
        players.forEach { it.send(packet) }
    }
    
    fun display(filter: (Player) -> Boolean) {
        val packet = build()
        location.world!!.players.forEach { if (filter(it)) it.send(packet) }
    }
    
    fun display(filter: Predicate<Player>) {
        val packet = build()
        location.world!!.players.forEach { if (filter.test(it)) it.send(packet) }
    }
    
}

fun <T : ParticleOptions> particle(particle: ParticleType<T>, location: Location, config: ParticleBuilder<T>.() -> Unit) =
    ParticleBuilder(particle, location).apply(config).build()

fun <T : ParticleOptions> particle(particle: ParticleType<T>, config: ParticleBuilder<T>.() -> Unit) =
    ParticleBuilder(particle).apply(config).build()

//<editor-fold desc="Options extension functions" defaultstate="collapsed">

fun ParticleBuilder<BlockParticleOption>.block(material: Material) = options {
    BlockParticleOption(it, material.nmsBlock.defaultBlockState())
}

fun ParticleBuilder<BlockParticleOption>.block(block: MojangBlock) = options {
    BlockParticleOption(it, block.defaultBlockState())
}

fun ParticleBuilder<BlockParticleOption>.block(blockState: MojangBlockState) = options {
    BlockParticleOption(it, blockState)
}

fun ParticleBuilder<DustParticleOptions>.dust(color: Color, size: Float = 1f) = options {
    DustParticleOptions(Vector3f(color.red / 255f, color.green / 255f, color.blue / 255f), size)
}

fun ParticleBuilder<DustParticleOptions>.dust(color: Vector3f, size: Float = 1f) = options {
    DustParticleOptions(color, size)
}

fun ParticleBuilder<DustParticleOptions>.color(color: Color) = dust(color)

fun ParticleBuilder<DustColorTransitionOptions>.dustTransition(from: Color, to: Color, size: Float = 1f) = options {
    DustColorTransitionOptions(Vector3f(from.red / 255f, from.green / 255f, from.blue / 255f),
        Vector3f(to.red / 255f, to.green / 255f, to.blue / 255f), size)
}

fun ParticleBuilder<DustColorTransitionOptions>.dustTransition(from: Vector3f, to: Vector3f, size: Float = 1f) = options {
    DustColorTransitionOptions(from, to, size)
}

fun ParticleBuilder<DustColorTransitionOptions>.color(from: Color, to: Color) = dustTransition(from, to)

fun ParticleBuilder<SculkChargeParticleOptions>.sculkCharge(roll: Float) = options {
    SculkChargeParticleOptions(roll)
}

fun ParticleBuilder<ItemParticleOption>.item(itemStack: ItemStack) = options {
    ItemParticleOption(it, itemStack.nmsStack)
}

fun ParticleBuilder<ItemParticleOption>.item(itemStack: MojangStack) = options {
    ItemParticleOption(it, itemStack)
}

fun ParticleBuilder<ItemParticleOption>.item(material: Material) = options {
    ItemParticleOption(it, MojangStack(CraftMagicNumbers.getItem(material)))
}

fun ParticleBuilder<ItemParticleOption>.item(item: Item) = options {
    ItemParticleOption(it, MojangStack(item))
}

fun ParticleBuilder<VibrationParticleOption>.vibration(destination: Entity, ticks: Int, yOffset: Float = 0f) = options {
    VibrationParticleOption(EntityPositionSource(destination.nmsEntity, yOffset), ticks)
}

fun ParticleBuilder<VibrationParticleOption>.vibration(destination: Location, ticks: Int) = options {
    VibrationParticleOption(BlockPositionSource(destination.blockPos), ticks)
}

fun ParticleBuilder<ShriekParticleOption>.shriek(delay: Int) = options {
    ShriekParticleOption(delay)
}

fun ParticleBuilder<SimpleParticleType>.noteColor(note: Int) {
    offsetX(note / 24f)
}

//</editor-fold>