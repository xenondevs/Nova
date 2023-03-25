@file:Suppress("unused", "MemberVisibilityCanBePrivate", "DEPRECATION")

package xyz.xenondevs.nova.material

import com.mojang.datafixers.util.Either
import net.minecraft.resources.ResourceLocation
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.item.ItemLogic
import xyz.xenondevs.nova.item.behavior.ItemBehaviorHolder
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.get
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.namespacedId
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.api.data.NamespacedId as INamespacedID
import xyz.xenondevs.nova.api.material.NovaMaterial as INovaMaterial
import xyz.xenondevs.nova.api.material.NovaMaterialRegistry as INovaMaterialRegistry

class LegacyMaterialWrapper(val material: Either<NovaItem, NovaBlock>) : INovaMaterial {
    
    override val id: INamespacedID
        get() = material.map(NovaItem::id, NovaBlock::id).namespacedId
    
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
    @Deprecated("")
    override fun getOrNull(id: INamespacedID): INovaMaterial? = getOrNull(id.toString())
    @Deprecated("")
    override fun getOrNull(item: ItemStack): INovaMaterial? = item.novaMaterial?.let { LegacyMaterialWrapper(Either.left(it)) }
    @Deprecated("")
    override fun get(id: String): INovaMaterial = getOrNull(id)!!
    @Deprecated("")
    override fun get(id: INamespacedID): INovaMaterial = getOrNull(id)!!
    @Deprecated("")
    override fun get(item: ItemStack): INovaMaterial = getOrNull(item)!!
    @Deprecated("")
    override fun getNonNamespaced(name: String): List<INovaMaterial> = NovaRegistries.ITEM.getByName(name).map { LegacyMaterialWrapper(Either.left(it)) }
    
    //<editor-fold desc="internal", defaultstate="collapsed">
    internal fun registerCoreItem(
        name: String,
        vararg itemBehaviors: ItemBehaviorHolder<*>,
        localizedName: String = "item.nova.$name",
        isHidden: Boolean = false
    ): NovaItem {
        return register(NovaItem(
            ResourceLocation("nova", name),
            localizedName,
            ItemLogic(*itemBehaviors),
            isHidden = isHidden
        ))
    }
    
    internal fun registerUnnamedHiddenCoreItem(name: String, localizedName: String = "", vararg itemBehaviors: ItemBehaviorHolder<*>): NovaItem {
        return register(NovaItem(
            ResourceLocation("nova", name),
            localizedName,
            ItemLogic(*itemBehaviors),
            isHidden = true
        ))
    }
    //</editor-fold>
    
    internal fun register(item: NovaItem): NovaItem {
        val id = item.id
        
        NovaRegistries.ITEM[id] = item
        return item
    }
    
}