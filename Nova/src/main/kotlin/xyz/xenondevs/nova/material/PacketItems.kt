package xyz.xenondevs.nova.material

import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData.DataItem
import net.minecraft.world.item.Items
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.network.event.impl.*
import com.mojang.datafixers.util.Pair as MojangPair
import net.minecraft.world.item.ItemStack as MojangStack

object PacketItems : Initializable(), Listener {
    
    override val inMainThread = true
    override val dependsOn = CustomItemServiceManager
    
    override fun init() {
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
    }
    
    @EventHandler
    fun handleSetContentPacket(event: ClientboundContainerSetContentPacketEvent) {
        val packet = event.packet
        val items = packet.items
        val carriedItem = packet.carriedItem
        var changed = false
        
        val newItems = items.map { item ->
            if (isNovaItem(item)) {
                changed = true
                return@map getFakeItem(item)
            } else return@map item
        }
        if (changed)
            event.items = newItems
        if (isNovaItem(carriedItem))
            event.carriedItem = getFakeItem(carriedItem)
    }
    
    @EventHandler
    fun handleSetSlotPacket(event: ClientboundContainerSetSlotPacketEvent) {
        val packet = event.packet
        val item = packet.item
        if (isNovaItem(item))
            event.item = getFakeItem(item)
    }
    
    @EventHandler
    fun handleEntityData(event: ClientboundSetEntityDataPacketEvent) {
        val packet = event.packet
        val data = packet.unpackedData ?: return
        val i = data.indexOfFirst { it.value is MojangStack }
        if (i == -1) return
        val dataItem = data[i]
        val item = dataItem.value as MojangStack
        if (isNovaItem(item)) {
            val newDataItem = DataItem(dataItem.accessor as EntityDataAccessor<MojangStack>, getFakeItem(item))
            data[i] = newDataItem
        }
    }
    
    @EventHandler
    fun handleSetEquipment(event: ClientboundSetEquipmentPacketEvent) {
        val packet = event.packet
        val slots = packet.slots
        var changed = false
        
        val newSlots = slots.map { slot ->
            if (isNovaItem(slot.second)) {
                changed = true
                return@map MojangPair(slot.first, getFakeItem(slot.second))
            } else return@map slot
        }
        if (changed) event.slots = newSlots
    }
    
    @EventHandler
    fun handleCreativeSetItem(event: ServerboundSetCreativeModeSlotPacketEvent) {
        val packet = event.packet
        val item = packet.item
        if (isFakeItem(item))
            event.item = getNovaItem(item)
    }
    
    private fun isNovaItem(item: MojangStack): Boolean {
        return false
    }
    
    private fun isFakeItem(item: MojangStack): Boolean {
        return false
    }
    
    private fun getNovaItem(item: MojangStack): MojangStack {
        return item
    }
    
    private fun getFakeItem(item: MojangStack): MojangStack {
        return item
    }
    
    
}