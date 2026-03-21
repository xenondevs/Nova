package xyz.xenondevs.nova.serialization.cbf

import io.papermc.paper.registry.RegistryKey
import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Cbf
import xyz.xenondevs.cbf.CbfSecurityManager
import xyz.xenondevs.cbf.serializer.BinarySerializer
import xyz.xenondevs.commons.reflection.classifierClass
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.serialization.cbf.NamespacedCompound.NamespacedCompoundBinarySerializer
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import kotlin.reflect.KType

internal object CbfSerializers {
    
    fun register() {
        Cbf.registerSerializer(NamespacedCompoundBinarySerializer)
        Cbf.registerSerializer(ColorBinarySerializer)
        Cbf.registerSerializer(LocationBinarySerializer)
        Cbf.registerSerializer(NamespacedKeyBinarySerializer)
        Cbf.registerSerializer(IdentifierBinarySerializer)
        Cbf.registerSerializer(KeyBinarySerializer)
        Cbf.registerSerializer(VirtualInventoryBinarySerializer)
        Cbf.registerSerializer(BlockPosBinarySerializer)
        Cbf.registerSerializer(MojangItemStackBinarySerializer)
        Cbf.registerSerializer(BukkitItemStackBinarySerializer)
        Cbf.registerSerializerFactory(ItemFilterBinarySerializerFactory)
        Cbf.registerSerializerFactory(TableBinarySerializer)
        
        Cbf.registerRegistrySerializers(NovaRegistries.BLOCK)
        Cbf.registerRegistrySerializers(NovaRegistries.ITEM)
        Cbf.registerRegistrySerializers(NovaRegistries.EQUIPMENT)
        Cbf.registerRegistrySerializers(NovaRegistries.TOOL_TIER)
        Cbf.registerRegistrySerializers(NovaRegistries.TOOL_CATEGORY)
        Cbf.registerRegistrySerializers(NovaRegistries.NETWORK_TYPE)
        Cbf.registerRegistrySerializers(NovaRegistries.ABILITY_TYPE)
        Cbf.registerRegistrySerializers(NovaRegistries.ATTACHMENT_TYPE)
        Cbf.registerRegistrySerializers(NovaRegistries.RECIPE_TYPE)
        Cbf.registerRegistrySerializers(NovaRegistries.GUI_TEXTURE)
        Cbf.registerRegistrySerializers(NovaRegistries.WAILA_INFO_PROVIDER)
        Cbf.registerRegistrySerializers(NovaRegistries.WAILA_TOOL_ICON_PROVIDER)
        Cbf.registerRegistrySerializers(NovaRegistries.ITEM_FILTER_TYPE)
        Cbf.registerRegistrySerializers(NovaRegistries.TOOLTIP_STYLE)
        
        Cbf.registerRegistrySerializers(RegistryKey.ATTRIBUTE)
        Cbf.registerRegistrySerializers(RegistryKey.BANNER_PATTERN)
        Cbf.registerRegistrySerializers(RegistryKey.BIOME)
        Cbf.registerRegistrySerializers(RegistryKey.BLOCK)
        Cbf.registerRegistrySerializers(RegistryKey.CAT_VARIANT)
        Cbf.registerRegistrySerializers(RegistryKey.CHICKEN_VARIANT)
        Cbf.registerRegistrySerializers(RegistryKey.COW_VARIANT)
        Cbf.registerRegistrySerializers(RegistryKey.DAMAGE_TYPE)
        Cbf.registerRegistrySerializers(RegistryKey.DATA_COMPONENT_TYPE)
        Cbf.registerRegistrySerializers(RegistryKey.DIALOG)
        Cbf.registerRegistrySerializers(RegistryKey.ENCHANTMENT)
        Cbf.registerRegistrySerializers(RegistryKey.ENTITY_TYPE)
        Cbf.registerRegistrySerializers(RegistryKey.FLUID)
        Cbf.registerRegistrySerializers(RegistryKey.FROG_VARIANT)
        Cbf.registerRegistrySerializers(RegistryKey.GAME_EVENT)
        Cbf.registerRegistrySerializers(RegistryKey.GAME_RULE)
        Cbf.registerRegistrySerializers(RegistryKey.INSTRUMENT)
        Cbf.registerRegistrySerializers(RegistryKey.ITEM)
        Cbf.registerRegistrySerializers(RegistryKey.JUKEBOX_SONG)
        Cbf.registerRegistrySerializers(RegistryKey.MAP_DECORATION_TYPE)
        Cbf.registerRegistrySerializers(RegistryKey.MEMORY_MODULE_TYPE)
        Cbf.registerRegistrySerializers(RegistryKey.MENU)
        Cbf.registerRegistrySerializers(RegistryKey.MOB_EFFECT)
        Cbf.registerRegistrySerializers(RegistryKey.PAINTING_VARIANT)
        Cbf.registerRegistrySerializers(RegistryKey.PARTICLE_TYPE)
        Cbf.registerRegistrySerializers(RegistryKey.PIG_VARIANT)
        Cbf.registerRegistrySerializers(RegistryKey.POTION)
        Cbf.registerRegistrySerializers(RegistryKey.SOUND_EVENT)
        Cbf.registerRegistrySerializers(RegistryKey.STRUCTURE)
        Cbf.registerRegistrySerializers(RegistryKey.STRUCTURE_TYPE)
        Cbf.registerRegistrySerializers(RegistryKey.TRIM_MATERIAL)
        Cbf.registerRegistrySerializers(RegistryKey.TRIM_PATTERN)
        Cbf.registerRegistrySerializers(RegistryKey.VILLAGER_PROFESSION)
        Cbf.registerRegistrySerializers(RegistryKey.VILLAGER_TYPE)
        Cbf.registerRegistrySerializers(RegistryKey.WOLF_SOUND_VARIANT)
        Cbf.registerRegistrySerializers(RegistryKey.WOLF_VARIANT)
        Cbf.registerRegistrySerializers(RegistryKey.ZOMBIE_NAUTILUS_VARIANT)
        
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