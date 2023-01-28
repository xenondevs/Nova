package xyz.xenondevs.nova.data.serialization.cbf

import de.studiocode.invui.virtualinventory.VirtualInventory
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.CBF.registerBinaryAdapter
import xyz.xenondevs.cbf.CBF.registerBinaryHierarchyAdapter
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.instancecreator.InstanceCreator
import xyz.xenondevs.cbf.security.CBFSecurityManager
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound.NamespacedCompoundBinaryAdapter
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
import kotlin.reflect.KClass

internal object CBFAdapters {
    
    fun register() {
        // binary adapters
        registerBinaryAdapter(NamespacedCompound::class, NamespacedCompoundBinaryAdapter)
        registerBinaryAdapter(Color::class, ColorBinaryAdapter)
        registerBinaryAdapter(Location::class, LocationBinaryAdapter)
        registerBinaryAdapter(NamespacedKey::class, NamespacedKeyBinaryAdapter)
        registerBinaryAdapter(NamespacedId::class, NamespacedIdBinaryAdapter)
        registerBinaryAdapter(NetworkType::class, NetworkTypeBinaryAdapter)
        registerBinaryAdapter(VirtualInventory::class, VirtualInventoryBinaryAdapter)
        registerBinaryAdapter(ItemFilter::class, ItemFilterBinaryAdapter)
        
        // binary hierarchy adapters
        registerBinaryHierarchyAdapter(ItemStack::class, ItemStackBinaryAdapter)
        
        // register security manager (this prevents addons from registering adapters / instance creators for non-addon classes)
        CBF.securityManager = CBFAddonSecurityManager()
    }
    
    private class CBFAddonSecurityManager : CBFSecurityManager {
        
        override fun <T : Any> canRegisterAdapter(clazz: KClass<T>, adapter: BinaryAdapter<T>): Boolean {
            return clazz.java.classLoader == adapter.javaClass.classLoader
        }
        
        override fun <T : Any> canRegisterHierarchyAdapter(clazz: KClass<T>, adapter: BinaryAdapter<T>): Boolean {
            return clazz.java.classLoader == adapter.javaClass.classLoader
        }
        
        override fun <T : Any> canRegisterInstanceCreator(clazz: KClass<T>, instanceCreator: InstanceCreator<T>): Boolean {
            return clazz.java.classLoader == instanceCreator.javaClass.classLoader
        }
        
    }
    
}