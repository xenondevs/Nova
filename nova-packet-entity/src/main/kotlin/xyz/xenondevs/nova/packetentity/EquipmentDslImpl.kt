package xyz.xenondevs.nova.packetentity

import com.mojang.datafixers.util.Pair
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import net.minecraft.world.entity.EquipmentSlot as NmsEquipmentSlot
import net.minecraft.world.item.ItemStack as NmsItemStack

internal class PacketEntityEquipmentState {
    
    val hand = DefaultEntityValue<ItemStack?>(null)
    val offHand = DefaultEntityValue<ItemStack?>(null)
    val feet = DefaultEntityValue<ItemStack?>(null)
    val legs = DefaultEntityValue<ItemStack?>(null)
    val chest = DefaultEntityValue<ItemStack?>(null)
    val head = DefaultEntityValue<ItemStack?>(null)
    val body = DefaultEntityValue<ItemStack?>(null)
    val saddle = DefaultEntityValue<ItemStack?>(null)
    
    fun observe(observer: () -> Unit) {
        hand.observe(observer)
        offHand.observe(observer)
        feet.observe(observer)
        legs.observe(observer)
        chest.observe(observer)
        head.observe(observer)
        body.observe(observer)
        saddle.observe(observer)
    }
    
    fun unobserve() {
        hand.unobserve()
        offHand.unobserve()
        feet.unobserve()
        legs.unobserve()
        chest.unobserve()
        head.unobserve()
        body.unobserve()
        saddle.unobserve()
    }
    
    fun buildPacket(entityId: Int): ClientboundSetEquipmentPacket? {
        val slots = buildList<Pair<NmsEquipmentSlot, NmsItemStack>> {
            hand.get()?.let { add(Pair(NmsEquipmentSlot.MAINHAND, CraftItemStack.unwrap(it))) }
            offHand.get()?.let { add(Pair(NmsEquipmentSlot.OFFHAND, CraftItemStack.unwrap(it))) }
            feet.get()?.let { add(Pair(NmsEquipmentSlot.FEET, CraftItemStack.unwrap(it))) }
            legs.get()?.let { add(Pair(NmsEquipmentSlot.LEGS, CraftItemStack.unwrap(it))) }
            chest.get()?.let { add(Pair(NmsEquipmentSlot.CHEST, CraftItemStack.unwrap(it))) }
            head.get()?.let { add(Pair(NmsEquipmentSlot.HEAD, CraftItemStack.unwrap(it))) }
            body.get()?.let { add(Pair(NmsEquipmentSlot.BODY, CraftItemStack.unwrap(it))) }
            saddle.get()?.let { add(Pair(NmsEquipmentSlot.SADDLE, CraftItemStack.unwrap(it))) }
        }
        return if (slots.isNotEmpty()) ClientboundSetEquipmentPacket(entityId, slots) else null
    }
    
}

internal class EquipmentDslImpl(
    private val equipment: PacketEntityEquipmentState
) : EquipmentDsl {
    
    override val hand get() = equipment.hand
    override val offHand get() = equipment.offHand
    override val feet get() = equipment.feet
    override val legs get() = equipment.legs
    override val chest get() = equipment.chest
    override val head get() = equipment.head
    override val body get() = equipment.body
    override val saddle get() = equipment.saddle
    
}

internal class EquipmentDslPropertyImpl(
    private val equipment: PacketEntityEquipmentState
) : EquipmentDslProperty {
    
    override fun get(slot: EquipmentSlot) = when (slot) {
        EquipmentSlot.HAND -> equipment.hand
        EquipmentSlot.OFF_HAND -> equipment.offHand
        EquipmentSlot.FEET -> equipment.feet
        EquipmentSlot.LEGS -> equipment.legs
        EquipmentSlot.CHEST -> equipment.chest
        EquipmentSlot.HEAD -> equipment.head
        EquipmentSlot.BODY -> equipment.body
        EquipmentSlot.SADDLE -> equipment.saddle
    }
    
}

internal class PacketEntityEquipmentImpl(
    equipment: PacketEntityEquipmentState
) : PacketEntityEquipment {
    
    override fun get(slot: EquipmentSlot): ItemStack? = when (slot) {
        EquipmentSlot.HAND -> hand
        EquipmentSlot.OFF_HAND -> offHand
        EquipmentSlot.FEET -> feet
        EquipmentSlot.LEGS -> legs
        EquipmentSlot.CHEST -> chest
        EquipmentSlot.HEAD -> head
        EquipmentSlot.BODY -> body
        EquipmentSlot.SADDLE -> saddle
    }
    
    override fun set(slot: EquipmentSlot, value: ItemStack?) {
        when (slot) {
            EquipmentSlot.HAND -> hand = value
            EquipmentSlot.OFF_HAND -> offHand = value
            EquipmentSlot.FEET -> feet = value
            EquipmentSlot.LEGS -> legs = value
            EquipmentSlot.CHEST -> chest = value
            EquipmentSlot.HEAD -> head = value
            EquipmentSlot.BODY -> body = value
            EquipmentSlot.SADDLE -> saddle = value
        }
    }
    
    override var hand by equipment.hand
    override var offHand by equipment.offHand
    override var feet by equipment.feet
    override var legs by equipment.legs
    override var chest by equipment.chest
    override var head by equipment.head
    override var body by equipment.body
    override var saddle by equipment.saddle
    
}
