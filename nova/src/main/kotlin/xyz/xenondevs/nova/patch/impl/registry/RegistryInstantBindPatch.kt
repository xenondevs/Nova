package xyz.xenondevs.nova.patch.impl.registry

import net.minecraft.core.Holder
import net.minecraft.core.MappedRegistry
import net.minecraft.core.registries.Registries
import org.objectweb.asm.Opcodes
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.replaceEvery
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.HOLDER_REFERENCE_BIND_VALUE_METHOD

internal object RegistryInstantBindPatch : MultiTransformer(MappedRegistry::class) {
    
    private val INSTANT_BIND_REGISTRIES = setOf(Registries.DATA_COMPONENT_TYPE)
    
    override fun transform() {
        VirtualClassPath[MappedRegistry<*>::register].instructions.replaceEvery(
            0, 0,
            {
                dup()
                aLoad(0) // this
                aLoad(2) // value
                invokeStatic(::instantBind)
                areturn()
            }
        ) { it.opcode == Opcodes.ARETURN }
    }
    
    @JvmStatic
    fun instantBind(reference: Holder.Reference<*>, registry: MappedRegistry<*>, value: Any) {
        if (registry.key() in INSTANT_BIND_REGISTRIES) {
            HOLDER_REFERENCE_BIND_VALUE_METHOD.invoke(reference, value)
        }
    }
    
}