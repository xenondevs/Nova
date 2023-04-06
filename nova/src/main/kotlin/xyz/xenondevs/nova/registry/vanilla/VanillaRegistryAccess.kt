package xyz.xenondevs.nova.registry.vanilla

import net.minecraft.core.HolderLookup
import net.minecraft.core.MappedRegistry
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.core.WritableRegistry
import net.minecraft.resources.ResourceKey
import xyz.xenondevs.nova.util.MINECRAFT_SERVER
import xyz.xenondevs.nova.util.NMSUtils
import java.util.*

object VanillaRegistryAccess : RegistryAccess by MINECRAFT_SERVER.registryAccess() {
    
    override fun <T : Any> lookup(key: ResourceKey<out Registry<out T>>): Optional<HolderLookup.RegistryLookup<T>> {
        return this.registry(key).map { if (it is MappedRegistry<T>) VanillaLookupWrapper(it) else it.asLookup() }
    }
    
    internal fun freezeAll() {
        registries().forEach { it.value.freeze() }
    }
    
    internal fun unfreezeAll() {
        registries().forEach { NMSUtils.unfreezeRegistry(it.value) }
    }
    
    override fun <E : Any> registryOrThrow(key: ResourceKey<out Registry<out E>>): WritableRegistry<E> {
        return super.registryOrThrow(key) as WritableRegistry<E>
    }
    
}