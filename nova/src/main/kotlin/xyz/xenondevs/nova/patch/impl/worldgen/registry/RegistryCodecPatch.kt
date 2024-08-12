package xyz.xenondevs.nova.patch.impl.worldgen.registry

import com.mojang.serialization.DynamicOps
import net.minecraft.core.DefaultedMappedRegistry
import net.minecraft.core.Holder
import net.minecraft.core.HolderGetter
import net.minecraft.core.HolderSet
import net.minecraft.core.MappedRegistry
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.RegistryFileCodec
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.replaceFirst
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.vanilla.VanillaRegistries
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getMethod
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.wrapper.WrapperBlock
import java.util.*

private val REGISTRY_OPS_GETTER_METHOD =
    getMethod(RegistryOps::class, "getter", ResourceKey::class)
private val REGISTRY_GET_HOLDER_METHOD =
    getMethod(Registry::class, "getHolder", ResourceLocation::class)
private val REGISTRY_FILE_CODEC_DECODE_METHOD =
    getMethod(RegistryFileCodec::class, "decode", DynamicOps::class, Any::class)
private val REGISTRY_REFERENCE_HOLDER_WITH_LIFECYCLE_LAMBDA =
    getMethod(Registry::class, "lambda\$referenceHolderWithLifecycle\$4", ResourceLocation::class)
private val DEFAULTED_MAPPED_REGISTRY_GET_METHOD =
    getMethod(DefaultedMappedRegistry::class, "get", ResourceLocation::class)

/**
 * Allows accessing Nova's registry from Minecraft's Block registry.
 */
@OptIn(ExperimentalWorldGen::class)
internal object RegistryCodecPatch : MultiTransformer(RegistryFileCodec::class, Registry::class, MappedRegistry::class, DefaultedMappedRegistry::class) {
    
    override fun transform() {
        patchRegistryFileCodec()
        patchRegistryReferenceHolderWithLifecycle()
        patchDefaultedMappedRegistry()
    }
    
    /**
     * Replaces the holder getter used in [RegistryFileCodec.decode] to a one that can produce [WrapperBlocks][WrapperBlock]
     * for nova blocks.
     */
    private fun patchRegistryFileCodec() {
        VirtualClassPath[REGISTRY_FILE_CODEC_DECODE_METHOD].instructions.replaceFirst(
            0, 0,
            buildInsnList {
                invokeStatic(::getter)
            }
        ) { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(REGISTRY_OPS_GETTER_METHOD) }
    }
    
    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun getter(registryOps: RegistryOps<*>, key: ResourceKey<Registry<*>>): Optional<HolderGetter<*>> {
        if (key != Registries.BLOCK)
            return registryOps.getter(key) as Optional<HolderGetter<*>>
        
        val getter = registryOps.getter(Registries.BLOCK).orElseThrow()
        val injectGetter = object : HolderGetter<Block> {
            
            override fun get(key: ResourceKey<Block>): Optional<Holder.Reference<Block>> {
                val vanilla = getter.get(key)
                if (vanilla.isPresent)
                    return vanilla
                return NovaRegistries.WRAPPER_BLOCK.getHolder(key.location()) as Optional<Holder.Reference<Block>>
            }
            
            override fun get(tag: TagKey<Block>): Optional<HolderSet.Named<Block>> {
                val vanilla = getter.get(tag)
                if (vanilla.isPresent)
                    return vanilla
                return NovaRegistries.WRAPPER_BLOCK.getHolder(tag.location()) as Optional<HolderSet.Named<Block>>
            }
            
        }
        
        return Optional.of(injectGetter)
    }
    
    /**
     * Intercepts a [Registry.getHolder] call in [Registry.referenceHolderWithLifecycle] to also produce [WrapperBlocks][WrapperBlock].
     */
    private fun patchRegistryReferenceHolderWithLifecycle() {
        // Mapping name will 100% change in the future, check for these params and method structure: https://i.imgur.com/4ix2zLq.png
        VirtualClassPath[REGISTRY_REFERENCE_HOLDER_WITH_LIFECYCLE_LAMBDA].instructions.replaceFirst(
            0, 0,
            buildInsnList {
                invokeStatic(::getHolder)
            }
        ) { it.opcode == Opcodes.INVOKEINTERFACE && (it as MethodInsnNode).calls(REGISTRY_GET_HOLDER_METHOD) }
    }
    
    @JvmStatic
    fun getHolder(registry: Registry<*>, id: ResourceLocation): Optional<out Holder.Reference<*>> {
        var holder: Optional<out Holder.Reference<*>> = registry.getHolder(id)
        if (holder.isEmpty && registry == VanillaRegistries.BLOCK) {
            holder = NovaRegistries.WRAPPER_BLOCK.getHolder(id)
        }
        
        return holder
    }
    
    private fun patchDefaultedMappedRegistry() {
        val instructions = VirtualClassPath[DEFAULTED_MAPPED_REGISTRY_GET_METHOD].instructions
        instructions.insert(buildInsnList {
            val continueLabel = instructions.first as LabelNode
            aLoad(1)
            invokeVirtual(ResourceLocation::getNamespace)
            ldc("minecraft")
            invokeVirtual(String::equals)
            ifne(continueLabel) // if location.namespace == "minecraft" goto continueLabel
            
            addLabel()
            constNull()
            areturn() // return null
        })
    }
    
}