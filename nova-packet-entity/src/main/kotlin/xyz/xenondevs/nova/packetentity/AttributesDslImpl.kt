package xyz.xenondevs.nova.packetentity

import net.minecraft.core.Holder
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket.AttributeSnapshot
import org.bukkit.attribute.Attribute
import org.bukkit.craftbukkit.attribute.CraftAttribute
import xyz.xenondevs.commons.collections.takeUnlessEmpty
import xyz.xenondevs.commons.provider.dsl.DslProperty
import java.util.concurrent.atomic.AtomicLong
import net.minecraft.world.entity.ai.attributes.Attribute as NmsAttribute

private data class AttributeEntry(
    val holder: Holder<NmsAttribute>,
    val index: Int,
    val value: DefaultEntityValue<Double?> = DefaultEntityValue(null)
) {
    
    fun toAttributeSnapshotOrNull(): AttributeSnapshot? =
        value.get()?.let { AttributeSnapshot(holder, it, emptyList()) }
    
    fun toAttributeSnapshot(): AttributeSnapshot =
        AttributeSnapshot(holder, value.get() ?: holder.value().defaultValue, emptyList())
    
}

internal class PacketEntityAttributes {
    
    private var entries: HashMap<Attribute, AttributeEntry>? = null
    private val dirtyAttributes = AtomicLong(0)
    
    @Volatile
    private var observer: (() -> Unit)? = null
    
    fun get(attribute: Attribute): DslProperty<Double?> {
        val entries = entries ?: HashMap<Attribute, AttributeEntry>().also { this.entries = it }
        return entries.getOrPut(attribute) {
            val entry = AttributeEntry((attribute as CraftAttribute).holder, entries.size)
            entry.value.observe {
                observer?.let { observer ->
                    markDirty(entry.index)
                    observer()
                }
            }
            entry
        }.value
    }
    
    fun buildFullPacket(entityId: Int): ClientboundUpdateAttributesPacket? = entries?.values
        ?.mapNotNull { it.toAttributeSnapshotOrNull() }
        ?.takeUnlessEmpty()
        ?.let { ClientboundUpdateAttributesPacket(entityId, it) }
    
    fun buildDirtyPacket(entityId: Int): ClientboundUpdateAttributesPacket? {
        val dirtyAttributes = dirtyAttributes.getAndSet(0)
        if (dirtyAttributes == 0L)
            return null
        
        return entries?.values?.mapNotNull { entry ->
            if ((dirtyAttributes and (1L shl entry.index)) != 0L)
                entry.toAttributeSnapshot()
            else null
        }?.takeUnlessEmpty()?.let { ClientboundUpdateAttributesPacket(entityId, it) }
    }
    
    fun observe(observer: () -> Unit) {
        this.observer = observer
    }
    
    fun unobserve() {
        observer = null
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
