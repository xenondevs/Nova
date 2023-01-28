package xyz.xenondevs.nova.transformer.patch.worldgen.registry

import net.minecraft.core.Holder
import net.minecraft.core.MappedRegistry
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.VarInsnNode
import xyz.xenondevs.bytebase.asm.OBJECT_TYPE
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.util.insertBeforeFirst
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.bytebase.util.next
import xyz.xenondevs.nova.transformer.MethodTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.MAPPED_REGISTRY_REGISTER_MAPPING_METHOD

/**
 * Mojang no longer binds the value of holders when registering something to a registry. So we wrap all values passed to
 * [MappedRegistry.registerMapping] in a [ValueWrapper] and inject a check to unwrap and bind the value.
 */
internal object MappedRegistryPatch : MethodTransformer(MAPPED_REGISTRY_REGISTER_MAPPING_METHOD) {
    
    private val VALUE_WRAPPER_NAME = ValueWrapper::class.internalName
    
    override fun transform() {
        methodNode.insertBeforeFirst(buildInsnList {
            val continueLabel = LabelNode()
            
            aLoad(3)
            instanceOf(VALUE_WRAPPER_NAME)
            ifeq(continueLabel) // if (!(value instanceof ValueWrapper)) goto continueLabel;
            
            addLabel()
            aLoad(3)
            checkCast(VALUE_WRAPPER_NAME)
            getField(VALUE_WRAPPER_NAME, "value", "L$OBJECT_TYPE;")
            aStore(3) // value = ((ValueWrapper) value).value;

            addLabel()
            aLoad(5) // ref
            aLoad(3) // value
            invokeVirtual(Holder.Reference::class.internalName, "SRM(net.minecraft.core.Holder\$Reference bindValue)", "(Ljava/lang/Object;)V")
            
            add(continueLabel)
        }) { insn -> // https://i.imgur.com/uLdd6pu.png
            insn.opcode == Opcodes.ALOAD && (insn as VarInsnNode).`var` == 0
                && insn.next?.let { it.opcode == Opcodes.GETFIELD && (it as FieldInsnNode).name == "SRF(net.minecraft.core.MappedRegistry byKey)" } == true
                && insn.next(2)?.let { it.opcode == Opcodes.ALOAD && (it as VarInsnNode).`var` == 2 } == true
                && insn.next(3)?.let { it.opcode == Opcodes.ALOAD && (it as VarInsnNode).`var` == 5 } == true
        }
    }
    
}