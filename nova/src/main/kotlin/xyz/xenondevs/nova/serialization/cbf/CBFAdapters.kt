package xyz.xenondevs.nova.serialization.cbf

import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.CBF.registerBinaryAdapter
import xyz.xenondevs.cbf.CBF.registerBinaryHierarchyAdapter
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.adapter.impl.EnumBinaryAdapter
import xyz.xenondevs.cbf.instancecreator.InstanceCreator
import xyz.xenondevs.cbf.security.CBFSecurityManager
import xyz.xenondevs.commons.reflection.classifierClass
import xyz.xenondevs.nova.registry.NovaRegistries.ABILITY_TYPE
import xyz.xenondevs.nova.registry.NovaRegistries.ATTACHMENT_TYPE
import xyz.xenondevs.nova.registry.NovaRegistries.BLOCK
import xyz.xenondevs.nova.registry.NovaRegistries.ITEM
import xyz.xenondevs.nova.registry.NovaRegistries.NETWORK_TYPE
import xyz.xenondevs.nova.registry.NovaRegistries.RECIPE_TYPE
import xyz.xenondevs.nova.registry.NovaRegistries.TOOL_CATEGORY
import xyz.xenondevs.nova.registry.NovaRegistries.TOOL_TIER
import xyz.xenondevs.nova.serialization.cbf.NamespacedCompound.NamespacedCompoundBinaryAdapter
import xyz.xenondevs.nova.serialization.cbf.adapter.BlockPosBinaryAdapter
import xyz.xenondevs.nova.serialization.cbf.adapter.BukkitItemStackBinaryAdapter
import xyz.xenondevs.nova.serialization.cbf.adapter.ColorBinaryAdapter
import xyz.xenondevs.nova.serialization.cbf.adapter.ItemFilterBinaryAdapter
import xyz.xenondevs.nova.serialization.cbf.adapter.LocationBinaryAdapter
import xyz.xenondevs.nova.serialization.cbf.adapter.MojangItemStackBinaryAdapter
import xyz.xenondevs.nova.serialization.cbf.adapter.NamespacedIdBinaryAdapter
import xyz.xenondevs.nova.serialization.cbf.adapter.NamespacedKeyBinaryAdapter
import xyz.xenondevs.nova.serialization.cbf.adapter.RegistryBinaryAdapter
import xyz.xenondevs.nova.serialization.cbf.adapter.ResourceLocationBinaryAdapter
import xyz.xenondevs.nova.serialization.cbf.adapter.TableBinaryAdapter
import xyz.xenondevs.nova.serialization.cbf.adapter.VirtualInventoryBinaryAdapter
import xyz.xenondevs.nova.serialization.cbf.adapter.byNameBinaryAdapter
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
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
        registerBinaryAdapter(ResourceLocationBinaryAdapter)
        registerBinaryAdapter(VirtualInventoryBinaryAdapter)
        registerBinaryAdapter(BlockPosBinaryAdapter)
        registerBinaryAdapter(MojangItemStackBinaryAdapter)
        registerBinaryAdapter(NETWORK_TYPE.byNameBinaryAdapter())
        registerBinaryAdapter(ABILITY_TYPE.byNameBinaryAdapter())
        registerBinaryAdapter(ATTACHMENT_TYPE.byNameBinaryAdapter())
        registerBinaryAdapter(RECIPE_TYPE.byNameBinaryAdapter())
        
        // binary hierarchy adapters
        registerBinaryHierarchyAdapter(TableBinaryAdapter)
        registerBinaryHierarchyAdapter(BukkitItemStackBinaryAdapter)
        registerBinaryHierarchyAdapter(ItemFilterBinaryAdapter)
        registerBinaryHierarchyAdapter(BLOCK.byNameBinaryAdapter())
        registerBinaryHierarchyAdapter(ITEM.byNameBinaryAdapter())
        registerBinaryHierarchyAdapter(TOOL_CATEGORY.byNameBinaryAdapter())
        registerBinaryHierarchyAdapter(TOOL_TIER.byNameBinaryAdapter())
        
        // register security manager (this prevents addons from registering adapters / instance creators for non-addon classes)
        CBF.securityManager = CBFAddonSecurityManager()
        
        // configure ordinal enums
        EnumBinaryAdapter.addOrdinalEnums(BlockFace::class, NetworkConnectionType::class)
    }
    
    private class CBFAddonSecurityManager : CBFSecurityManager {
        
        override fun <T : Any> canRegisterAdapter(type: KType, adapter: BinaryAdapter<T>): Boolean {
            if (adapter is RegistryBinaryAdapter)
                return true
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