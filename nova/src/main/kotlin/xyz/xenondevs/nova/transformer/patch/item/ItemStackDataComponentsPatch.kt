@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.transformer.patch.item

import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.core.component.PatchedDataComponentMap
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.replaceFirst
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.legacy.ItemStackLegacyConversion
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.data.getCompoundOrNull
import xyz.xenondevs.nova.util.get
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import java.util.logging.Level
import kotlin.jvm.optionals.getOrNull

private val ITEM_STACK_CONSTRUCTOR = ReflectionUtils.getConstructor(
    ItemStack::class,
    Holder::class, Int::class, DataComponentPatch::class
)

internal object ItemStackDataComponentsPatch : MultiTransformer(ItemStack::class) {
    
    override fun transform() {
        VirtualClassPath[ITEM_STACK_CONSTRUCTOR].replaceFirst(0, 0, buildInsnList {
            invokeStatic(::fromPatch)
        }) { it.opcode == Opcodes.INVOKESTATIC && (it as MethodInsnNode).calls(PatchedDataComponentMap::fromPatch) }
    }
    
    @JvmStatic
    fun fromPatch(base: DataComponentMap, changes: DataComponentPatch): PatchedDataComponentMap {
        val patch = convertLegacy(changes)
        
        val novaItem = patch.get(DataComponents.CUSTOM_DATA)?.getOrNull()?.unsafe
            ?.getCompoundOrNull("nova")
            ?.getString("id")
            ?.let(NovaRegistries.ITEM::get)
        
        return PatchedDataComponentMap.fromPatch(
            if (novaItem != null) NovaDataComponentMap(novaItem) else base,
            patch
        )
    }
    
    private fun convertLegacy(patch: DataComponentPatch): DataComponentPatch {
        val unsafeCustomTag = patch.get(DataComponents.CUSTOM_DATA)
            ?.getOrNull()
            ?.unsafe
            ?: return patch // not a nova item
        
        val novaId = unsafeCustomTag
            .getCompoundOrNull("nova")
            ?.getString("id")
            ?: return patch // not a nova item
        
        val customTag = unsafeCustomTag.copy()
        ItemStackLegacyConversion.convert(customTag, novaId)
        
        return DataComponentPatch.builder()
            .apply { copy(patch) }
            .set(DataComponents.CUSTOM_DATA, CustomData.of(customTag))
            .build()
    }
    
}

// this delegating structure is necessary to allow config reloading
internal class NovaDataComponentMap(private val novaItem: NovaItem) : DataComponentMap {
    
    override fun <T : Any?> get(type: DataComponentType<out T>): T? {
        try {
            return novaItem.baseDataComponents.get(type)
        } catch (e: Exception) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve base data components for $novaItem", e)
        }
        
        return null
    }
    
    override fun keySet(): Set<DataComponentType<*>> {
        try {
            return novaItem.baseDataComponents.keySet()
        } catch (e: Exception) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve base data components for $novaItem", e)
        }
        
        return emptySet()
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as NovaDataComponentMap
        
        return novaItem == other.novaItem
    }
    
    override fun hashCode(): Int {
        return novaItem.hashCode()
    }
    
}