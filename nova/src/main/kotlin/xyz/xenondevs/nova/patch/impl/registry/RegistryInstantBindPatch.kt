package xyz.xenondevs.nova.patch.impl.registry

import net.minecraft.core.Holder
import net.minecraft.core.MappedRegistry
import net.minecraft.core.registries.Registries
import org.bukkit.craftbukkit.CraftRegistry
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.replaceEvery
import xyz.xenondevs.bytebase.util.replaceFirst
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.HOLDER_REFERENCE_BIND_VALUE_METHOD

internal object RegistryInstantBindPatch : MultiTransformer(MappedRegistry::class, CraftRegistry::class) {
    
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
        
        // Suppresses precondition "java.lang.IllegalStateException: Registry is already loaded"
        VirtualClassPath[CraftRegistry<*, *>::lockReferenceHolders].instructions.replaceFirst(0, 0, buildInsnList {
            pop() // map
            ldc(1)
        }) { it.opcode == Opcodes.INVOKEINTERFACE && (it as MethodInsnNode).calls(Map<*, *>::isEmpty) }
    }
    
    @JvmStatic
    fun instantBind(reference: Holder.Reference<*>, registry: MappedRegistry<*>, value: Any) {
        if (registry.key() in INSTANT_BIND_REGISTRIES) {
            HOLDER_REFERENCE_BIND_VALUE_METHOD.invoke(reference, value)
        }
    }
    
}