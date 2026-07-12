@file:Suppress("unused", "MemberVisibilityCanBePrivate", "DEPRECATION", "DeprecatedCallableAddReplaceWith")

package xyz.xenondevs.nova.api

import com.mojang.datafixers.util.Either
import net.minecraft.core.registries.BuiltInRegistries
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.component.adventure.toPlainText
import xyz.xenondevs.nova.util.getValue
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.name
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.name
import xyz.xenondevs.nova.world.item.novaItem
import xyz.xenondevs.nova.api.data.NamespacedId as INamespacedId
import xyz.xenondevs.nova.api.material.NovaMaterial as INovaMaterial
import xyz.xenondevs.nova.api.material.NovaMaterialRegistry as INovaMaterialRegistry

internal class LegacyMaterialWrapper(val material: Either<NovaItem, NovaBlock>) : INovaMaterial {
    
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    override fun getId(): INamespacedId {
        val map = material.map(NovaItem::key, NovaBlock::key)
        return NamespacedId(map.namespace(), map.value())
    }
    
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    override fun getLocalizedName(locale: String): String {
        val component = material.map({it.entry.get().name}, {it.entry.get().name})
        return component?.toPlainText(locale) ?: ""
    }
    
}

internal object NovaMaterialRegistry : INovaMaterialRegistry {
    
    @Deprecated("")
    override fun getOrNull(id: String): INovaMaterial? {
        val novaItem = (BuiltInRegistries.ITEM.getValue(id) as? NovaItem)
        if (novaItem != null) return LegacyMaterialWrapper(Either.left(novaItem))
        val novaBlock = (BuiltInRegistries.BLOCK.getValue(id) as? NovaBlock)
        if (novaBlock != null) return LegacyMaterialWrapper(Either.right(novaBlock))
        return null
    }
    
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    override fun getOrNull(id: INamespacedId): INovaMaterial? = getOrNull(id.toString())
    
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    override fun getOrNull(item: ItemStack): INovaMaterial? = item.novaItem?.let { LegacyMaterialWrapper(Either.left(it)) }
    
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    override fun get(id: String): INovaMaterial = getOrNull(id)!!
    
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    override fun get(id: INamespacedId): INovaMaterial = getOrNull(id)!!
    
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    override fun get(item: ItemStack): INovaMaterial = getOrNull(item)!!
    
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    override fun getNonNamespaced(name: String): List<INovaMaterial> = BuiltInRegistries.ITEM.entrySet()
        .filter { [key, item] -> item is NovaItem && key.identifier().path == name }
        .map { [_, item] -> LegacyMaterialWrapper(Either.left(item as NovaItem)) }
    
}