package xyz.xenondevs.nova.data.serialization.cbf

import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.CBF.registerBinaryAdapter
import xyz.xenondevs.cbf.CBF.registerBinaryHierarchyAdapter
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.instancecreator.InstanceCreator
import xyz.xenondevs.cbf.security.CBFSecurityManager
import xyz.xenondevs.commons.reflection.classifierClass
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound.NamespacedCompoundBinaryAdapter
import xyz.xenondevs.nova.data.serialization.cbf.adapter.ColorBinaryAdapter
import xyz.xenondevs.nova.data.serialization.cbf.adapter.ItemFilterBinaryAdapter
import xyz.xenondevs.nova.data.serialization.cbf.adapter.ItemStackBinaryAdapter
import xyz.xenondevs.nova.data.serialization.cbf.adapter.LocationBinaryAdapter
import xyz.xenondevs.nova.data.serialization.cbf.adapter.NamespacedIdBinaryAdapter
import xyz.xenondevs.nova.data.serialization.cbf.adapter.NamespacedKeyBinaryAdapter
import xyz.xenondevs.nova.data.serialization.cbf.adapter.NetworkTypeBinaryAdapter
import xyz.xenondevs.nova.data.serialization.cbf.adapter.UpgradeTypeBinaryAdapter
import xyz.xenondevs.nova.data.serialization.cbf.adapter.VirtualInventoryBinaryAdapter
import kotlin.reflect.KClass
import kotlin.reflect.KType

internal object CBFAdapters {
    
    fun register() {
        // binary adapters
        registerBinaryAdapter(NamespacedCompoundBinaryAdapter)
        registerBinaryAdapter(ColorBinaryAdapter)
        registerBinaryAdapter(LocationBinaryAdapter)
        registerBinaryAdapter(NamespacedKeyBinaryAdapter)
        registerBinaryAdapter(NamespacedIdBinaryAdapter)
        registerBinaryAdapter(NetworkTypeBinaryAdapter)
        registerBinaryAdapter(UpgradeTypeBinaryAdapter)
        registerBinaryAdapter(VirtualInventoryBinaryAdapter)
        registerBinaryAdapter(ItemFilterBinaryAdapter)
        
        // binary hierarchy adapters
        registerBinaryHierarchyAdapter(ItemStackBinaryAdapter)
        
        // register security manager (this prevents addons from registering adapters / instance creators for non-addon classes)
        CBF.securityManager = CBFAddonSecurityManager()
    }
    
    private class CBFAddonSecurityManager : CBFSecurityManager {
        
        override fun <T : Any> canRegisterAdapter(type: KType, adapter: BinaryAdapter<T>): Boolean {
            return type.classifierClass!!.java.classLoader == adapter.javaClass.classLoader
        }
        
        override fun <T : Any> canRegisterHierarchyAdapter(type: KType, adapter: BinaryAdapter<T>): Boolean {
            return type.classifierClass!!.java.classLoader == adapter.javaClass.classLoader
        }
        
        override fun <T : Any> canRegisterInstanceCreator(clazz: KClass<T>, instanceCreator: InstanceCreator<T>): Boolean {
            return clazz.java.classLoader == instanceCreator.javaClass.classLoader
        }
        
    }
    
}