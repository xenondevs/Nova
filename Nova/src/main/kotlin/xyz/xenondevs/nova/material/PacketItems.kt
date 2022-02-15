package xyz.xenondevs.nova.material

import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData.DataItem
import net.minecraft.world.item.Items
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_18_R1.util.CraftMagicNumbers
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.network.event.impl.*
import xyz.xenondevs.nova.util.data.NBTUtils
import com.mojang.datafixers.util.Pair as MojangPair
import net.minecraft.world.item.ItemStack as MojangStack

object PacketItems : Initializable(), Listener {
    
    private val MISSING_ITEM = MojangStack(Items.BARRIER).apply {
        this.hoverName = TranslatableComponent("item.nova.missing")
    }
    
    val SERVER_SIDE_MATERIAL = Material.SHULKER_SHELL
    val SERVER_SIDE_ITEM = CraftMagicNumbers.getItem(SERVER_SIDE_MATERIAL)!!
    
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
        return item.item == SERVER_SIDE_ITEM
            && item.tag != null
            && item.tag!!.contains("nova", NBTUtils.TAG_COMPOUND)
    }
    
    private fun isFakeItem(item: MojangStack): Boolean {
        return item.tag != null
            && item.tag!!.contains("nova", NBTUtils.TAG_COMPOUND)
            && item.tag!!.contains("CustomModelData", NBTUtils.TAG_INT)
    }
    
    private fun getNovaItem(item: MojangStack): MojangStack {
        return item.apply {
            this.item = SERVER_SIDE_ITEM
            tag?.remove("CustomModelData")
        }
    }
    
    private fun getFakeItem(item: MojangStack): MojangStack {
        val novaTag = item.tag!!.getCompound("nova")
            ?: throw IllegalStateException("Item is not a Nova item!")
        val id = novaTag.getString("id") ?: return MISSING_ITEM
        val subId = novaTag.getInt("subId")
        val isBlock = novaTag.getBoolean("isBlock")
        
        val (itemData, blockData) = Resources.getModelDataOrNull(id) ?: return MISSING_ITEM
        val data = (if (isBlock) blockData else itemData) ?: return MISSING_ITEM
        
        val newItem = item.copy()
        newItem.item = CraftMagicNumbers.getItem(data.material)
        newItem.tag!!.putInt("CustomModelData", data.dataArray[subId])
        
        return newItem
    }
}