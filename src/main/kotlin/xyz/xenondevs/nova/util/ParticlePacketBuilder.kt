package xyz.xenondevs.nova.util

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.particle.ParticleBuilder
import xyz.xenondevs.particle.ParticleEffect
import xyz.xenondevs.particle.PropertyType
import xyz.xenondevs.particle.data.ParticleData
import xyz.xenondevs.particle.data.VibrationData
import xyz.xenondevs.particle.data.color.DustColorTransitionData
import xyz.xenondevs.particle.data.color.DustData
import xyz.xenondevs.particle.data.texture.BlockTexture
import xyz.xenondevs.particle.data.texture.ItemTexture
import java.awt.Color

class ParticlePacketBuilder(val effect: ParticleEffect, location: Location? = null) {
    
    val builder = ParticleBuilder(effect, location)
    val packet: Any
        get() = builder.toPacket()
    
    fun location(location: Location): ParticlePacketBuilder {
        builder.setLocation(location)
        return this
    }
    
    fun location(world: World, x: Double, y: Double, z: Double): ParticlePacketBuilder {
        builder.setLocation(Location(world, x, y, z))
        return this
    }
    
    fun amount(amount: Int): ParticlePacketBuilder {
        builder.setAmount(amount)
        return this
    }
    
    fun offset(x: Float, y: Float, z: Float): ParticlePacketBuilder {
        builder.setOffset(x, y, z)
        return this
    }
    
    fun offset(x: Double, y: Double, z: Double): ParticlePacketBuilder {
        builder.setOffset(x.toFloat(), y.toFloat(), z.toFloat())
        return this
    }
    
    fun offsetX(offset: Float): ParticlePacketBuilder {
        builder.setOffsetX(offset)
        return this
    }
    
    fun offsetY(offset: Float): ParticlePacketBuilder {
        builder.setOffsetY(offset)
        return this
    }
    
    fun offsetZ(offset: Float): ParticlePacketBuilder {
        builder.setOffsetZ(offset)
        return this
    }
    
    fun speed(speed: Float): ParticlePacketBuilder {
        builder.setSpeed(speed)
        return this
    }
    
    fun data(data: ParticleData): ParticlePacketBuilder {
        builder.setParticleData(data)
        return this
    }
    
    fun color(color: Color): ParticlePacketBuilder {
        builder.setColor(color)
        return this
    }
    
    fun texture(material: Material): ParticlePacketBuilder {
        when {
            effect.hasProperty(PropertyType.REQUIRES_BLOCK) -> data(BlockTexture(material))
            effect.hasProperty(PropertyType.REQUIRES_ITEM) -> data(ItemTexture(ItemStack(material)))
        }
        
        return this
    }
    
    fun texture(item: ItemStack): ParticlePacketBuilder {
        builder.setParticleData(ItemTexture(item))
        return this
    }
    
    fun dust(color: Color, size: Float): ParticlePacketBuilder {
        builder.setParticleData(DustData(color, size))
        return this
    }
    
    fun dustFade(color: Color, fadeColor: Color, size: Float): ParticlePacketBuilder {
        builder.setParticleData(DustColorTransitionData(color, fadeColor, size))
        return this
    }
    
    fun vibration(start: Location, dest: Location, ticks: Int): ParticlePacketBuilder {
        builder.setParticleData(VibrationData(start, dest, ticks))
        return this
    }
    
    fun display() = builder.display()
    
    fun display(player: Player) = builder.display(player)
    
    fun display(vararg players: Player) = builder.display(players.toList())
    
    fun display(players: Collection<Player>) = builder.display(players)
    
    fun display(predicate: (Player) -> Boolean) = builder.display(predicate)
}

fun particle(effect: ParticleEffect, builder: ParticlePacketBuilder.() -> Unit): Any =
    ParticlePacketBuilder(effect).apply(builder).packet

fun particleBuilder(effect: ParticleEffect, location: Location? = null, builder: ParticlePacketBuilder.() -> Unit) =
    ParticlePacketBuilder(effect, location).apply(builder)