package xyz.xenondevs.nova.transformer.patch.worldgen.registry

import com.mojang.serialization.DataResult
import com.mojang.serialization.Lifecycle
import net.minecraft.core.MappedRegistry
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
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
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.MAPPED_REGISTRY_LIFECYCLE_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.REGISTRY_BY_NAME_CODEC_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.REGISTRY_FILE_CODEC_DECODE_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.world.generation.inject.codec.BlockNovaMaterialDecoder
import xyz.xenondevs.nova.world.generation.wrapper.WrapperBlock
import com.mojang.datafixers.util.Pair as MojangPair

/**
 * Allows accessing Nova's registry from Minecraft's Block registry.
 */
internal object RegistryCodecPatch : MultiTransformer(setOf(RegistryFileCodec::class, Registry::class, MappedRegistry::class), true) {
    
    private val RESOURCE_KEY_NAME = ResourceKey::class.internalName
    private val RESOURCE_LOCATION_NAME = ResourceLocation::class.internalName
    
    override fun transform() {
        patchRegistryFileCodec()
        patchRegistryByNameCodec()
        patchRegistryLifecycleGetter()
    }
    
    /**
     * Inserts instructions into RegistryFileCodec to check if a non minecraft block is requested and if so, checks if that
     * block is registered in Nova and returns a [WrapperBlock] instead.
     */
    private fun patchRegistryFileCodec() {
        val methodNode = VirtualClassPath[REGISTRY_FILE_CODEC_DECODE_METHOD]
        // For future reference: https://i.imgur.com/Agm0yYI.png
        methodNode.insertAfterFirst(buildInsnList {
            val continueLabel = methodNode.instructions.findNthOfType<LabelNode>(11) // L12 in the image
            
            addLabel()
            aLoad(0) // this
            getField(RegistryFileCodec::class.internalName, "SRF(net.minecraft.resources.RegistryFileCodec registryKey)", "L$RESOURCE_KEY_NAME;")
            getStatic(Registries::class.internalName, "SRF(net.minecraft.core.registries.Registries BLOCK)", "L$RESOURCE_KEY_NAME;")
            invokeVirtual("java/lang/Object", "equals", "(Ljava/lang/Object;)Z")
            ifeq(continueLabel) // if registryKey != Registry.BLOCK goto continueLabel
            
            addLabel()
            aLoad(7) // pair
            invokeVirtual(MojangPair::class.internalName, "getFirst", "()Ljava/lang/Object;") // datafixers isn't obfuscated
            checkCast(RESOURCE_KEY_NAME)
            invokeVirtual(RESOURCE_KEY_NAME, "SRM(net.minecraft.resources.ResourceKey location)", "()L$RESOURCE_LOCATION_NAME;")
            dup()
            aStore(10) // var location = pair.getFirst().location()
            invokeVirtual(RESOURCE_LOCATION_NAME, "SRM(net.minecraft.resources.ResourceLocation getNamespace)", "()Ljava/lang/String;")
            ldc("minecraft")
            invokeVirtual("java/lang/String", "equals", "(Ljava/lang/Object;)Z")
            ifne(continueLabel) // if location.namespace != "minecraft" goto continueLabel
            
            addLabel()
            aLoad(10) // location
            aLoad(2) // obj (to decode)
            invokeStatic(ReflectionUtils.getMethod(BlockNovaMaterialDecoder::class.java, false, "decodeToPair", ResourceLocation::class.java, Object::class.java)) // DataResult<Pair<Holder<Block>, T>>
            dup()
            aStore(11) // var decodedPair = BlockNovaMaterialDecoder.decodeToPair(location, obj)
            invokeVirtual(DataResult::class.internalName, "result", "()Ljava/util/Optional;")
            invokeVirtual("java/util/Optional", "isPresent", "()Z")
            ifeq(continueLabel) // if !decodedPair.result().isPresent() goto continueLabel
            
            addLabel()
            aLoad(11)
            areturn() // return decodedPair
        }) { it.opcode == Opcodes.ASTORE && (it as VarInsnNode).`var` == 7 }
    }
    
    /**
     * Same as [patchRegistryFileCodec] but for the RegistryByNameCodec.
     */
    private fun patchRegistryByNameCodec() {
        // Mapping name will 100% change in the future, check for these params and method structure: https://i.imgur.com/5mD0ET7.png
        val instructions = VirtualClassPath[REGISTRY_BY_NAME_CODEC_METHOD].instructions
        instructions.insert(buildInsnList {
            val continueLabel = instructions.first as LabelNode
            
            addLabel()
            aLoad(0)
            invokeInterface(Registry::class.internalName, "SRM(net.minecraft.core.Registry key)", "()L$RESOURCE_KEY_NAME;")
            getStatic(Registries::class.internalName, "SRF(net.minecraft.core.registries.Registries BLOCK)", "L$RESOURCE_KEY_NAME;")
            invokeVirtual("java/lang/Object", "equals", "(Ljava/lang/Object;)Z")
            ifeq(continueLabel) // if registryKey != Registry.BLOCK goto continueLabel
            
            addLabel()
            aLoad(1)
            invokeVirtual(RESOURCE_LOCATION_NAME, "SRM(net.minecraft.resources.ResourceLocation getNamespace)", "()Ljava/lang/String;")
            ldc("minecraft")
            invokeVirtual("java/lang/String", "equals", "(Ljava/lang/Object;)Z")
            ifne(continueLabel) // if location.namespace != "minecraft" goto continueLabel
            
            addLabel()
            aLoad(1)
            invokeStatic(ReflectionUtils.getMethod(BlockNovaMaterialDecoder::class.java, false, "decodeToBlock", ResourceLocation::class.java)) // DataResult<Block>
            dup()
            aStore(2) // var decodedBlock = BlockNovaMaterialDecoder.decodeToBlock(location)
            invokeVirtual(DataResult::class.internalName, "result", "()Ljava/util/Optional;")
            invokeVirtual("java/util/Optional", "isPresent", "()Z")
            ifeq(continueLabel) // if !decodedBlock.result().isPresent() goto continueLabel
            
            addLabel()
            aLoad(2)
            areturn() // return decodedBlock
        })
    }
    
    /**
     * Ensures that all [WrapperBlock]s are marked as stable.
     */
    private fun patchRegistryLifecycleGetter() {
        val lifecycleName = Lifecycle::class.internalName
        // In case of mapping change, method body should be "return this.lifecycles.get(param0)"
        val instructions = VirtualClassPath[MAPPED_REGISTRY_LIFECYCLE_METHOD].instructions
        instructions.insert(buildInsnList {
            val continueLabel = instructions.first as LabelNode
            aLoad(1)
            instanceOf(WrapperBlock::class.internalName)
            ifeq(continueLabel) // if !(obj instanceof WrapperBlock) goto continueLabel
            addLabel()
            invokeStatic(lifecycleName, "stable", "()L$lifecycleName;")
            areturn() // return Lifecycle.stable()
        })
    }
    
}