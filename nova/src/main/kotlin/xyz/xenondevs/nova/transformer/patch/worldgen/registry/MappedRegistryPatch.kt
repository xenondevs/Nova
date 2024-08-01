package xyz.xenondevs.nova.transformer.patch.worldgen.registry

import net.minecraft.core.Holder
import net.minecraft.core.MappedRegistry
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.insertBeforeFirst
import xyz.xenondevs.nova.transformer.MethodTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.reflection.ReflectionUtils

private val MAP_PUT = ReflectionUtils.getMethod(Map::class, "put", Any::class, Any::class)

/**
 * Mojang no longer binds the value of holders when registering something to a registry. So we wrap all values passed to
 * [MappedRegistry.register] in a [InstantBindValue] and inject a check to unwrap and bind the value.
 */
internal object MappedRegistryPatch : MethodTransformer(MappedRegistry<*>::register) {
    
    override fun transform() {
        methodNode.insertBeforeFirst(buildInsnList {
            aLoad(4) // holder
            aLoad(2) // value
            invokeStatic(::unwrapAndBind)
            aStore(2) // unwrapped value
        }) { // https://i.imgur.com/kIbTk0Y.png
            it.opcode == Opcodes.INVOKEINTERFACE && (it as MethodInsnNode).calls(MAP_PUT)
        }
    }
    
    @JvmStatic
    fun unwrapAndBind(holder: Holder.Reference<Any?>, value: Any?): Any? {
        if (value is InstantBindValue) {
            ReflectionRegistry.HOLDER_REFERENCE_BIND_VALUE_METHOD.invoke(holder, value.value)
            return value.value
        }
        
        return value
    }
    
}