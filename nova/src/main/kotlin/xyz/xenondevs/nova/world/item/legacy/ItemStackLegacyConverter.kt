package xyz.xenondevs.nova.world.item.legacy

import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.ByteArrayTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.enchantment.ItemEnchantments
import org.bukkit.NamespacedKey
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.nova.registry.vanilla.VanillaRegistries
import xyz.xenondevs.nova.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.util.data.getByteArrayOrNull
import xyz.xenondevs.nova.util.data.getIntOrNull
import xyz.xenondevs.nova.util.data.getOrNull
import xyz.xenondevs.nova.util.getHolderOrThrow
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.KType

internal sealed interface ItemStackLegacyConverter {
    
    val affectedItemIds: Set<String>?
        get() = null
    
    fun convert(patch: DataComponentPatch): DataComponentPatch
    
}

internal abstract class ItemStackTagLegacyConverter : ItemStackLegacyConverter {
    
    final override fun convert(patch: DataComponentPatch): DataComponentPatch {
        val tag = patch.get(DataComponents.CUSTOM_DATA)?.getOrNull()?.copyTag()
            ?: return patch
        
        convert(tag)
        
        return DataComponentPatch.builder().apply {
            copy(patch)
            set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
        }.build()
    }
    
    abstract fun convert(tag: CompoundTag)
    
}

internal class ItemStackNamespaceConverter(
    override val affectedItemIds: Set<String>,
    private val newNamespace: String
) : ItemStackTagLegacyConverter() {
    
    override fun convert(tag: CompoundTag) {
        val novaTag = tag.getOrNull<CompoundTag>("nova") ?: return
        novaTag.putString("id", newNamespace + ":" + novaTag.getString("id").split(':')[1])
    }
    
}

internal class ItemStackPersistentDataConverter(
    private val type: KType,
    private val oldKey: NamespacedKey,
    private val newKey: NamespacedKey = oldKey
) : ItemStackTagLegacyConverter() {
    
    override fun convert(tag: CompoundTag) {
        val serializedValue = tag
            .getOrNull<CompoundTag>("PublicBukkitValues")
            ?.getOrNull<ByteArrayTag>(oldKey.toString())
            ?.asByteArray
            ?: return
        val novaCompound = tag
            .getByteArrayOrNull("nova_cbf")
            ?.let(CBF::read)
            ?: NamespacedCompound()
        novaCompound.set(type, newKey, CBF.read(type, serializedValue))
        tag.putByteArray("nova_cbf", CBF.write(novaCompound))
    }
    
}

internal data object ItemStackSubIdToModelIdConverter : ItemStackTagLegacyConverter() {
    
    override fun convert(tag: CompoundTag) {
        val novaTag = tag.getOrNull<CompoundTag>("nova")
            ?: return
        val subId = novaTag.getIntOrNull("subId")
        if (subId == null || subId == 0) {
            novaTag.remove("subId")
            novaTag.putString("modelId", "default")
        }
    }
    
}

internal data object ItemStackNovaDamageConverter : ItemStackLegacyConverter {
    
    @Suppress("DEPRECATION")
    override fun convert(patch: DataComponentPatch): DataComponentPatch {
        val unsafeTag = patch.get(DataComponents.CUSTOM_DATA)?.getOrNull()?.unsafe
            ?: return patch
        val novaCompound: NamespacedCompound = unsafeTag.getByteArrayOrNull("nova_cbf")?.let(CBF::read)
            ?: return patch
        val damage = novaCompound.get<Int>("nova", "damage")
            ?: return patch
       
        novaCompound.remove("nova", "damage")
       
        val tag = unsafeTag.copy()
        if (novaCompound.isNotEmpty()) {
            tag.putByteArray("nova_cbf", CBF.write(novaCompound))
        } else {
            tag.remove("nova_cbf")
        }
        
        return DataComponentPatch.builder().apply { 
            copy(patch)
            set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
            set(DataComponents.DAMAGE, damage)
        }.build()
    }
    
}

internal data object ItemStackEnchantmentsConverter : ItemStackLegacyConverter {
    
    @Suppress("DEPRECATION")
    override fun convert(patch: DataComponentPatch): DataComponentPatch {
        val unsafeTag = patch.get(DataComponents.CUSTOM_DATA)?.getOrNull()?.unsafe
            ?: return patch
        val novaCompound: NamespacedCompound = unsafeTag.getByteArrayOrNull("nova_cbf")?.let(CBF::read)
            ?: return patch
        
        val builder = DataComponentPatch.builder()
        builder.copy(patch)
        
        val enchantments: Map<ResourceLocation, Int>? = novaCompound["nova", "enchantments"]
        val storedEnchantments: Map<ResourceLocation, Int>? = novaCompound["nova", "stored_enchantments"]
        
        if (enchantments == null && storedEnchantments == null)
            return patch // nothing to convert
        
        fun convertEnchantments(component: DataComponentType<ItemEnchantments>, legacy: Map<ResourceLocation, Int>) {
            val componentEnchantments = patch.get(component)?.getOrNull() ?: ItemEnchantments.EMPTY
            val mutableEnchantments = ItemEnchantments.Mutable(componentEnchantments)
            for ((id, level) in legacy) {
                mutableEnchantments.set(VanillaRegistries.ENCHANTMENT.getHolderOrThrow(id), level)
            }
            builder.set(component, mutableEnchantments.toImmutable())
        }
        
        if (enchantments != null)
            convertEnchantments(DataComponents.ENCHANTMENTS, enchantments)
        if (storedEnchantments != null)
            convertEnchantments(DataComponents.STORED_ENCHANTMENTS, storedEnchantments)
        
        // remove legacy enchantment entries from patch
        val tag = unsafeTag.copy()
        novaCompound.remove("nova", "enchantments")
        novaCompound.remove("nova", "stored_enchantments")
        if (novaCompound.isNotEmpty()) {
            tag.putByteArray("nova_cbf", CBF.write(novaCompound))
        } else {
            tag.remove("nova_cbf")
        }
        builder.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
        
        return builder.build()
    }
    
}