package xyz.xenondevs.nova.tileentity

import org.bukkit.block.BlockFace
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.network.*
import xyz.xenondevs.nova.tileentity.network.energy.holder.EnergyHolder
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.util.emptyEnumMap
import xyz.xenondevs.nova.util.handItems
import xyz.xenondevs.nova.util.novaMaterial
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.actualDelegate
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class NetworkedTileEntity(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : TileEntity(uuid, data, material, ownerUUID, armorStand), NetworkEndPoint {
    
    final override val networks: MutableMap<NetworkType, MutableMap<BlockFace, Network>> =
        NetworkType.values().associateWithTo(emptyEnumMap()) { emptyEnumMap() }
    final override val connectedNodes: MutableMap<NetworkType, MutableMap<BlockFace, NetworkNode>> =
        NetworkType.values().associateWithTo(emptyEnumMap()) { emptyEnumMap() }
    final override val holders: MutableMap<NetworkType, EndPointDataHolder> by lazy {
        val map: EnumMap<NetworkType, EndPointDataHolder> = emptyEnumMap()
        if (::energyHolder.actualDelegate !is PlaceholderProperty) map[NetworkType.ENERGY] = energyHolder
        if (::itemHolder.actualDelegate !is PlaceholderProperty) map[NetworkType.ITEMS] = itemHolder
        return@lazy map
    }
    
    open val energyHolder: EnergyHolder by PlaceholderProperty
    open val itemHolder: ItemHolder by PlaceholderProperty
    
    override fun saveData() {
        super.saveData()
        holders.values.forEach(EndPointDataHolder::saveData)
    }
    
    override fun handleInitialized(first: Boolean) {
        NetworkManager.runAsync { it.handleEndPointAdd(this) }
    }
    
    final override fun handleRightClick(event: PlayerInteractEvent) {
        if (event.handItems.any { it.novaMaterial == NovaMaterialRegistry.WRENCH }) {
            event.isCancelled = true
            val face = event.blockFace
            
            NetworkManager.runAsync {
                val itemHolder = holders[NetworkType.ITEMS]
                if (itemHolder is ItemHolder)
                    itemHolder.cycleItemConfig(it, face, true)
            }
        } else handleRightClickNoWrench(event)
    }
    
    open fun handleRightClickNoWrench(event: PlayerInteractEvent) {
        super.handleRightClick(event)
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        
        val task: NetworkManagerTask = { it.handleEndPointRemove(this, unload) }
        if (NOVA.isEnabled) NetworkManager.runAsync(task) else NetworkManager.runNow(task)
    }
    
    override fun destroy(dropItems: Boolean): ArrayList<ItemStack> {
        val items = super.destroy(dropItems)
        if (dropItems) {
            val itemHolder = holders[NetworkType.ITEMS]
            if (itemHolder is ItemHolder) {
                items += (itemHolder.insertFilters.values.asSequence() + itemHolder.extractFilters.values.asSequence())
                    .map(ItemFilter::createFilterItem)
                
                itemHolder.insertFilters.clear()
                itemHolder.extractFilters.clear()
            }
        }
        
        return items
    }
    
}

private object PlaceholderProperty : ReadOnlyProperty<Any?, Nothing> {
    
    override fun getValue(thisRef: Any?, property: KProperty<*>): Nothing {
        throw UnsupportedOperationException("PlaceholderProperty cannot be read")
    }
    
}