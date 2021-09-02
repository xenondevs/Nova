package xyz.xenondevs.nova.tileentity

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.network.*
import xyz.xenondevs.nova.tileentity.network.energy.holder.EnergyHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.util.emptyEnumMap
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.actualDelegate
import xyz.xenondevs.nova.util.runAsyncTask
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
        runAsyncTask { NetworkManager.handleEndPointAdd(this) }
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        NetworkManager.handleEndPointRemove(this, unload)
    }
    
}

private object PlaceholderProperty : ReadOnlyProperty<Any?, Nothing> {
    
    override fun getValue(thisRef: Any?, property: KProperty<*>): Nothing {
        throw UnsupportedOperationException("PlaceholderProperty cannot be read")
    }
    
}