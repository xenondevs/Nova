package xyz.xenondevs.nova.packetentity

import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket.AttributeSnapshot
import org.bukkit.attribute.Attribute
import org.bukkit.craftbukkit.attribute.CraftAttribute
import xyz.xenondevs.commons.collections.takeUnlessEmpty
import xyz.xenondevs.commons.provider.dsl.DslProperty
import java.util.concurrent.atomic.AtomicLong
import net.minecraft.world.entity.ai.attributes.Attribute as NmsAttribute

private val ATTRIBUTES: List<Pair<Attribute, Holder<NmsAttribute>>> = BuiltInRegistries.ATTRIBUTE.stream()
    .map {
        val holder = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(it)
        val bukkit = CraftAttribute.minecraftHolderToBukkit(holder)
        bukkit to holder
    }.toList()

private data class AttributeEntry(
    val attribute: Attribute,
    val holder: Holder<NmsAttribute>,
    val value: DefaultEntityValue<Double?> = DefaultEntityValue(null)
) {
    
    fun toAttributeSnapshotOrNull(): AttributeSnapshot? =
        value.get()?.let { AttributeSnapshot(holder, it, emptyList()) }
    
    fun toAttributeSnapshot(): AttributeSnapshot =
        AttributeSnapshot(holder, value.get() ?: holder.value().defaultValue, emptyList())
    
}

internal class PacketEntityAttributes {
    
    private val entries = ATTRIBUTES.map { [bukkit, holder] -> AttributeEntry(bukkit, holder) }
    private val entriesByAttribute = entries.associateBy(AttributeEntry::attribute)
    private val dirtyAttributes = AtomicLong(0)
    
    fun get(attribute: Attribute): DslProperty<Double?> =
        entriesByAttribute.getValue(attribute).value
    
    fun buildFullPacket(entityId: Int): ClientboundUpdateAttributesPacket? = entries
        .mapNotNull { it.toAttributeSnapshotOrNull() }
        .takeUnlessEmpty()
        ?.let { ClientboundUpdateAttributesPacket(entityId, it) }
    
    fun buildDirtyPacket(entityId: Int): ClientboundUpdateAttributesPacket? {
        val dirtyAttributes = dirtyAttributes.getAndSet(0)
        if (dirtyAttributes == 0L)
            return null
        
        return entries.mapIndexedNotNull { i, entry ->
            if ((dirtyAttributes and (1L shl i)) != 0L)
                entry.toAttributeSnapshot()
            else null
        }.takeUnlessEmpty()?.let { ClientboundUpdateAttributesPacket(entityId, it) }
    }
    
    fun observe(observer: () -> Unit) {
        for ([i, entry] in entries.withIndex()) {
            entry.value.observe {
                markDirty(i)
                observer()
            }
        }
    }
    
    fun unobserve() {
        for (entry in entries) {
            entry.value.unobserve()
        }
    }
    
    private fun markDirty(i: Int) {
        require(i in 0..63)
        dirtyAttributes.getAndUpdate { it or (1L shl i) }
    }
    
}

internal class AttributesDslPropertyImpl(
    private val attributes: PacketEntityAttributes
) : AttributesDslProperty {
    
    override fun get(attribute: Attribute): DslProperty<Double?> =
        attributes.get(attribute)
    
}
