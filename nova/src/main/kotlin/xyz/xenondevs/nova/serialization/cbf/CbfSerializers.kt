package xyz.xenondevs.nova.serialization.cbf

import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Cbf
import xyz.xenondevs.cbf.CbfSecurityManager
import xyz.xenondevs.cbf.serializer.BinarySerializer
import xyz.xenondevs.commons.reflection.classifierClass
import xyz.xenondevs.nova.registry.NovaRegistries.ABILITY_TYPE
import xyz.xenondevs.nova.registry.NovaRegistries.ATTACHMENT_TYPE
import xyz.xenondevs.nova.registry.NovaRegistries.BLOCK
import xyz.xenondevs.nova.registry.NovaRegistries.ITEM
import xyz.xenondevs.nova.registry.NovaRegistries.NETWORK_TYPE
import xyz.xenondevs.nova.registry.NovaRegistries.RECIPE_TYPE
import xyz.xenondevs.nova.registry.NovaRegistries.TOOL_CATEGORY
import xyz.xenondevs.nova.registry.NovaRegistries.TOOL_TIER
import xyz.xenondevs.nova.serialization.cbf.NamespacedCompound.NamespacedCompoundBinarySerializer
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import kotlin.reflect.KType

internal object CbfSerializers {
    
    fun register() {
        Cbf.registerSerializer(NamespacedCompoundBinarySerializer)
        Cbf.registerSerializer(ColorBinarySerializer)
        Cbf.registerSerializer(LocationBinarySerializer)
        Cbf.registerSerializer(NamespacedKeyBinarySerializer)
        Cbf.registerSerializer(NamespacedIdBinarySerializer)
        Cbf.registerSerializer(ResourceLocationBinarySerializer)
        Cbf.registerSerializer(KeyBinarySerializer)
        Cbf.registerSerializer(VirtualInventoryBinarySerializer)
        Cbf.registerSerializer(BlockPosBinarySerializer)
        Cbf.registerSerializer(MojangItemStackBinarySerializer)
        Cbf.registerSerializer(BukkitItemStackBinarySerializer)
        Cbf.registerSerializer(NETWORK_TYPE.byNameBinarySerializer())
        Cbf.registerSerializer(ABILITY_TYPE.byNameBinarySerializer())
        Cbf.registerSerializer(ATTACHMENT_TYPE.byNameBinarySerializer())
        Cbf.registerSerializer(RECIPE_TYPE.byNameBinarySerializer())
        Cbf.registerSerializer(BLOCK.byNameBinarySerializer())
        Cbf.registerSerializer(ITEM.byNameBinarySerializer())
        Cbf.registerSerializer(TOOL_CATEGORY.byNameBinarySerializer())
        Cbf.registerSerializer(TOOL_TIER.byNameBinarySerializer())
        Cbf.registerSerializerFactory(ItemFilterBinarySerializerFactory)
        Cbf.registerSerializerFactory(TableBinarySerializer)
        
        Cbf.addOrdinalEnums(BlockFace::class, NetworkConnectionType::class)
        
        // this prevents addons from registering adapters / instance creators for non-addon classes
        Cbf.setSecurityManager(CbfAddonSecurityManager())
    }
    
    private class CbfAddonSecurityManager : CbfSecurityManager {
        
        override fun <T : Any> isAllowed(type: KType, serializer: BinarySerializer<T>): Boolean {
            val serializerClassName = serializer::class.qualifiedName!!
            if (serializerClassName.startsWith("xyz.xenondevs.cbf") || serializerClassName.startsWith("xyz.xenondevs.nova.serialization.cbf"))
                return true
            
            return type.classifierClass!!.java.classLoader == serializer.javaClass.classLoader
        }
        
    }
    
}