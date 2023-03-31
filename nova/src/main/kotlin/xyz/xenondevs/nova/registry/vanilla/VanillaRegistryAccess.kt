package xyz.xenondevs.nova.registry.vanilla

import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.core.WritableRegistry
import net.minecraft.resources.ResourceKey
import xyz.xenondevs.nova.util.MINECRAFT_SERVER
import xyz.xenondevs.nova.util.NMSUtils

object VanillaRegistryAccess : RegistryAccess by MINECRAFT_SERVER.registryAccess() {
    
    internal fun freezeAll() {
        registries().forEach { NMSUtils.freezeRegistry(it.value) }
    }
    
    internal fun unfreezeAll() {
        registries().forEach { NMSUtils.unfreezeRegistry(it.value) }
    }
    
    override fun <E : Any> registryOrThrow(key: ResourceKey<out Registry<out E>>): WritableRegistry<E> {
        return super.registryOrThrow(key) as WritableRegistry<E>
    }
    
}