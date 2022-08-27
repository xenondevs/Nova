package xyz.xenondevs.nova.data.serialization.cbf

import de.studiocode.invui.virtualinventory.VirtualInventory
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.CBF.registerBinaryAdapter
import xyz.xenondevs.cbf.CBF.registerBinaryHierarchyAdapter
import xyz.xenondevs.cbf.adapter.NettyBufferProvider
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.serialization.cbf.adapter.ColorBinaryAdapter
import xyz.xenondevs.nova.data.serialization.cbf.adapter.ItemFilterBinaryAdapter
import xyz.xenondevs.nova.data.serialization.cbf.adapter.ItemStackBinaryAdapter
import xyz.xenondevs.nova.data.serialization.cbf.adapter.LocationBinaryAdapter
import xyz.xenondevs.nova.data.serialization.cbf.adapter.NamespacedIdBinaryAdapter
import xyz.xenondevs.nova.data.serialization.cbf.adapter.NamespacedKeyBinaryAdapter
import xyz.xenondevs.nova.data.serialization.cbf.adapter.NetworkTypeBinaryAdapter
import xyz.xenondevs.nova.data.serialization.cbf.adapter.VirtualInventoryBinaryAdapter
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter
import java.awt.Color

internal object CBFAdapters {
    
    fun registerExtraAdapters() {
        CBF.defaultBufferProvider = NettyBufferProvider
        
        // binary adapters
        registerBinaryAdapter(Color::class, ColorBinaryAdapter)
        registerBinaryAdapter(Location::class, LocationBinaryAdapter)
        registerBinaryAdapter(NamespacedKey::class, NamespacedKeyBinaryAdapter)
        registerBinaryAdapter(NamespacedId::class, NamespacedIdBinaryAdapter)
        registerBinaryAdapter(NetworkType::class, NetworkTypeBinaryAdapter)
        registerBinaryAdapter(VirtualInventory::class, VirtualInventoryBinaryAdapter)
        registerBinaryAdapter(ItemFilter::class, ItemFilterBinaryAdapter)
        
        // binary hierarchy adapters
        registerBinaryHierarchyAdapter(ItemStack::class, ItemStackBinaryAdapter)
    }
    
}