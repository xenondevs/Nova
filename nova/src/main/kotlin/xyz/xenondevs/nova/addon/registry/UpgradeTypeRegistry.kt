package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.set
import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface UpgradeTypeRegistry : AddonGetter {
    
    fun <T> registerUpgradeType(name: String, item: NovaItem, icon: NovaItem, valueType: KType): UpgradeType<T> {
        val id = ResourceLocation(addon, name)
        val upgradeType = UpgradeType<T>(id, item, icon, valueType)
        
        NovaRegistries.UPGRADE_TYPE[id] = upgradeType
        return upgradeType
    }
    
}

inline fun <reified T> UpgradeTypeRegistry.registerUpgradeType(name: String, item: NovaItem, icon: NovaItem): UpgradeType<T> {
    return registerUpgradeType(name, item, icon, typeOf<T>())
}