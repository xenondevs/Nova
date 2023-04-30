@file:Suppress("unused", "MemberVisibilityCanBePrivate", "DEPRECATION", "DeprecatedCallableAddReplaceWith")

package xyz.xenondevs.nova.api

import com.mojang.datafixers.util.Either
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.get
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.namespacedId
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.api.data.NamespacedId as INamespacedID
import xyz.xenondevs.nova.api.material.NovaMaterial as INovaMaterial
import xyz.xenondevs.nova.api.material.NovaMaterialRegistry as INovaMaterialRegistry

internal class LegacyMaterialWrapper(val material: Either<NovaItem, NovaBlock>) : INovaMaterial {
    
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    override fun getId(): INamespacedID = material.map(NovaItem::id, NovaBlock::id).namespacedId
    
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    override fun getLocalizedName(locale: String): String {
        val key = material.map(NovaItem::localizedName, NovaBlock::localizedName)
        return LocaleManager.getTranslation(key, locale)
    }
    
}

internal object NovaMaterialRegistry : INovaMaterialRegistry {
    
    @Deprecated("")
    override fun getOrNull(id: String): INovaMaterial? {
        val novaItem = NovaRegistries.ITEM[id]
        if (novaItem != null) return LegacyMaterialWrapper(Either.left(novaItem))
        val novaBlock = NovaRegistries.BLOCK[id]
        if (novaBlock != null) return LegacyMaterialWrapper(Either.right(novaBlock))
        return null
    }
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    override fun getOrNull(id: INamespacedID): INovaMaterial? = getOrNull(id.toString())
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    override fun getOrNull(item: ItemStack): INovaMaterial? = item.novaItem?.let { LegacyMaterialWrapper(Either.left(it)) }
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    override fun get(id: String): INovaMaterial = getOrNull(id)!!
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    override fun get(id: INamespacedID): INovaMaterial = getOrNull(id)!!
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    override fun get(item: ItemStack): INovaMaterial = getOrNull(item)!!
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    override fun getNonNamespaced(name: String): List<INovaMaterial> = NovaRegistries.ITEM.getByName(name).map { LegacyMaterialWrapper(Either.left(it)) }
    
}