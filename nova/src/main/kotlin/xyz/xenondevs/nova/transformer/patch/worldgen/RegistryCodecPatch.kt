package xyz.xenondevs.nova.transformer.patch.worldgen

import com.mojang.serialization.DataResult
import com.mojang.serialization.Lifecycle
import net.minecraft.core.MappedRegistry
import net.minecraft.core.Registry
import net.minecraft.resources.RegistryFileCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.VarInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.insertAfterFirst
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.findNthOfType
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.world.generation.inject.codec.BlockNovaMaterialDecoder
import xyz.xenondevs.nova.world.generation.wrapper.WrapperBlock
import com.mojang.datafixers.util.Pair as MojangPair

internal object RegistryCodecPatch : MultiTransformer(setOf(RegistryFileCodec::class, Registry::class, MappedRegistry::class), true) {
    
    private val RESOURCE_KEY_NAME = ResourceKey::class.internalName
    private val RESOURCE_LOCATION_NAME = ResourceLocation::class.internalName
    
    override fun transform() {
        patchRegistryFileCodec()
        patchRegistryByNameCodec()
        patchRegistryLifecycleGetter()
    }
    
    private fun patchRegistryFileCodec() {
        val methodNode = VirtualClassPath[RegistryFileCodec::class].getMethod("SRM(net.minecraft.resources.RegistryFileCodec decode)")!!
        methodNode.insertAfterFirst(buildInsnList {
            val continueLabel = methodNode.instructions.findNthOfType<LabelNode>(11)
        
            addLabel()
            aLoad(0)
            getField(RegistryFileCodec::class.internalName, "SRF(net.minecraft.resources.RegistryFileCodec registryKey)", "L$RESOURCE_KEY_NAME;")
            getStatic(Registry::class.internalName, "SRF(net.minecraft.core.Registry BLOCK_REGISTRY)", "L$RESOURCE_KEY_NAME;")
            invokeVirtual("java/lang/Object", "equals", "(Ljava/lang/Object;)Z")
            ifeq(continueLabel)
        
            addLabel()
            aLoad(7)
            invokeVirtual(MojangPair::class.internalName, "getFirst", "()Ljava/lang/Object;") // datafixers isn't obfuscated
            checkCast(RESOURCE_KEY_NAME)
            invokeVirtual(RESOURCE_KEY_NAME, "SRM(net.minecraft.resources.ResourceKey location)", "()L$RESOURCE_LOCATION_NAME;")
            dup()
            aStore(10)
            invokeVirtual(RESOURCE_LOCATION_NAME, "SRM(net.minecraft.resources.ResourceLocation getNamespace)", "()Ljava/lang/String;")
            ldc("minecraft")
            invokeVirtual("java/lang/String", "equals", "(Ljava/lang/Object;)Z")
            ifne(continueLabel)
        
            addLabel()
            aLoad(10)
            aLoad(2)
            invokeStatic(ReflectionUtils.getMethod(BlockNovaMaterialDecoder::class.java, false, "decodeToPair", ResourceLocation::class.java, Object::class.java))
            dup()
            aStore(11)
            invokeVirtual(DataResult::class.internalName, "result", "()Ljava/util/Optional;")
            invokeVirtual("java/util/Optional", "isPresent", "()Z")
            ifeq(continueLabel)
        
            addLabel()
            aLoad(11)
            areturn()
        }) { it.opcode == Opcodes.ASTORE && (it as VarInsnNode).`var` == 7 }
    }
    
    private fun patchRegistryByNameCodec() {
        val instructions = VirtualClassPath[Registry::class].getMethod("SRM(net.minecraft.core.Registry lambda\$byNameCodec\$58)")!!.instructions
        instructions.insert(buildInsnList {
            val continueLabel = instructions.first as LabelNode
            
            addLabel()
            aLoad(0)
            getField(Registry::class.internalName, "SRF(net.minecraft.core.Registry key)", "L$RESOURCE_KEY_NAME;")
            getStatic(Registry::class.internalName, "SRF(net.minecraft.core.Registry BLOCK_REGISTRY)", "L$RESOURCE_KEY_NAME;")
            invokeVirtual("java/lang/Object", "equals", "(Ljava/lang/Object;)Z")
            ifeq(continueLabel)
            
            addLabel()
            aLoad(1)
            invokeVirtual(RESOURCE_LOCATION_NAME, "SRM(net.minecraft.resources.ResourceLocation getNamespace)", "()Ljava/lang/String;")
            ldc("minecraft")
            invokeVirtual("java/lang/String", "equals", "(Ljava/lang/Object;)Z")
            ifne(continueLabel)
            
            addLabel()
            aLoad(1)
            invokeStatic(ReflectionUtils.getMethod(BlockNovaMaterialDecoder::class.java, false, "decodeToBlock", ResourceLocation::class.java))
            dup()
            aStore(2)
            invokeVirtual(DataResult::class.internalName, "result", "()Ljava/util/Optional;")
            invokeVirtual("java/util/Optional", "isPresent", "()Z")
            ifeq(continueLabel)
            
            addLabel()
            aLoad(2)
            areturn()
        })
    }
    
    private fun patchRegistryLifecycleGetter() {
        val lifecycleName = Lifecycle::class.internalName
        val instructions = VirtualClassPath[MappedRegistry::class].getMethod("SRM(net.minecraft.core.MappedRegistry lifecycle)", "(Ljava/lang/Object;)L$lifecycleName;")!!.instructions
        instructions.insert(buildInsnList {
            val continueLabel = instructions.first as LabelNode
            aLoad(1)
            instanceOf(WrapperBlock::class.internalName)
            ifeq(continueLabel)
            addLabel()
            invokeStatic(lifecycleName, "stable", "()L$lifecycleName;")
            areturn()
        })
    }
    
}