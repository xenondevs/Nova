package xyz.xenondevs.nova.data.serialization.cbf

import de.studiocode.invui.virtualinventory.VirtualInventory
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.CBF.registerBinaryAdapter
import xyz.xenondevs.cbf.adapter.NettyBufferProvider
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.serialization.cbf.adapter.*
import xyz.xenondevs.nova.tileentity.network.NetworkType
import java.awt.Color

internal object CBFAdapters {
    
    fun registerExtraAdapters() {
        CBF.defaultBufferProvider = NettyBufferProvider
        registerBinaryAdapter(Color::class, ColorBinaryAdapter)
        registerBinaryAdapter(ItemStack::class, ItemStackBinaryAdapter)
        registerBinaryAdapter(Location::class, LocationBinaryAdapter)
        registerBinaryAdapter(NamespacedKey::class, NamespacedKeyBinaryAdapter)
        registerBinaryAdapter(NamespacedId::class, NamespacedIdBinaryAdapter)
        registerBinaryAdapter(NetworkType::class, NetworkTypeBinaryAdapter)
        registerBinaryAdapter(VirtualInventory::class, VirtualInventoryBinaryAdapter)
    }
    
}