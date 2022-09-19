package xyz.xenondevs.nova.transformer.patch.worldgen

import net.minecraft.world.level.chunk.GlobalPalette
import net.minecraft.world.level.chunk.HashMapPalette
import net.minecraft.world.level.chunk.LinearPalette
import net.minecraft.world.level.chunk.SingleValuePalette
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.asm.OBJECT_TYPE
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.util.insertAfterFirst
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.world.generation.wrapper.WrapperBlock

// TODO stringremapper for method instructions
internal object PalettePatch : MultiTransformer(
    classes = setOf(
        GlobalPalette::class,
        HashMapPalette::class,
        LinearPalette::class,
        SingleValuePalette::class
    ),
    computeFrames = true
) {
    
    private const val SINGLE_VALUE_PALETTE_NAME = "SRC/(net.minecraft.world.level.chunk.SingleValuePalette)"
    private const val LINEAR_PALETTE_NAME = "SRC/(net.minecraft.world.level.chunk.LinearPalette)"
    private const val HASH_MAP_PALETTE_NAME = "SRC/(net.minecraft.world.level.chunk.HashMapPalette)"
    private const val GLOBAL_PALETTE_NAME = "SRC/(net.minecraft.world.level.chunk.GlobalPalette)"
    
    private const val ID_MAP_NAME = "SRC/(net.minecraft.core.IdMap)"
    private const val FRIENDLY_BYTE_BUF_NAME = "SRC/(net.minecraft.network.FriendlyByteBuf)"
    
    private val WRAPPER_DESC = Type.getDescriptor(WrapperBlock::class.java)
    private val WRAPPER_INTERNAL_NAME = WrapperBlock::class.internalName
    
    override fun transform() {
        patchSingleValue()
        patchLinearPalette()
        patchHashMapPalette()
        patchGlobalPalette()
    }
    
    private fun patchSingleValue() {
        val clazz = classWrappers[SINGLE_VALUE_PALETTE_NAME]!!
        clazz.getMethod("SRM(net.minecraft.world.level.chunk.SingleValuePalette write)")!!.instructions = buildInsnList {
            // Object o;
            // if ((o = this.value) == null) {
            //     throw new IllegalStateException("Use of an uninitialized palette");
            // }
            // if (o instanceof final WrapperBlock wrapperBlock) {
            //     o = wrapperBlock.getDelegate();
            // }
            // var0.writeVarInt(this.registry.getId(o));
            val wrapperCheck = LabelNode()
            val byteBufferWriter = LabelNode()
            
            // L1
            addLabel()
            aLoad(0)
            getField(SINGLE_VALUE_PALETTE_NAME, "value", "L$OBJECT_TYPE;")
            dup()
            aStore(2)
            ifnonnull(wrapperCheck)
            
            // L2
            addLabel()
            new("java/lang/IllegalStateException")
            dup()
            ldc("Use of an uninitialized palette")
            invokeSpecial("java/lang/IllegalStateException", "<init>", "(Ljava/lang/String;)V")
            aThrow()
            
            // L3
            add(wrapperCheck)
            aLoad(2)
            instanceOf(WRAPPER_INTERNAL_NAME)
            ifeq(byteBufferWriter)
            
            // L4
            addLabel()
            aLoad(2)
            checkCast(WRAPPER_INTERNAL_NAME)
            invokeVirtual(WRAPPER_INTERNAL_NAME, "getDelegate", "()LSRC/(net.minecraft.world.level.block.Block);")
            aStore(2)
            
            // L5
            add(byteBufferWriter)
            aLoad(1)
            aLoad(0)
            getField(SINGLE_VALUE_PALETTE_NAME, "registry", "L$ID_MAP_NAME;")
            aLoad(2)
            invokeInterface(ID_MAP_NAME, "getId", "(Ljava/lang/Object;)I", true)
            invokeVirtual(FRIENDLY_BYTE_BUF_NAME, "writeVarInt", "(I)L$FRIENDLY_BYTE_BUF_NAME;")
            pop()
            
            // L6
            addLabel()
            _return()
        }
    }
    
    private fun patchLinearPalette() {
        val clazz = classWrappers[LINEAR_PALETTE_NAME]!!
        val idLabel = LabelNode()
        clazz.getMethod("SRM(net.minecraft.world.level.chunk.LinearPalette write)")!!.insertAfterFirst(buildInsnList {
            // if (object instanceof WrapperBlock) {
            //     object = ((WrapperBlock) object).getDelegate();
            // }
            dup()
            instanceOf(WRAPPER_INTERNAL_NAME)
            ifeq(idLabel)
            addLabel()
            checkCast(WRAPPER_INTERNAL_NAME)
            invokeVirtual(WRAPPER_INTERNAL_NAME, "getDelegate", "()LSRC/(net.minecraft.world.level.block.Block);")
            add(idLabel)
        }) { it.opcode == Opcodes.AALOAD }
    }
    
    private fun patchHashMapPalette() {
        val clazz = classWrappers[HASH_MAP_PALETTE_NAME]!!
        val idLabel = LabelNode()
        clazz.getMethod("SRM(net.minecraft.world.level.chunk.HashMapPalette write)")!!.insertAfterFirst(buildInsnList {
            // if (object instanceof WrapperBlock) {
            //     object = ((WrapperBlock) object).getDelegate();
            // }
            dup()
            instanceOf(WRAPPER_INTERNAL_NAME)
            ifeq(idLabel)
            addLabel()
            checkCast(WRAPPER_INTERNAL_NAME)
            invokeVirtual(WRAPPER_INTERNAL_NAME, "getDelegate", "()LSRC/(net.minecraft.world.level.block.Block);")
            add(idLabel)
        }) { it is MethodInsnNode && it.name == "byId" }
    }
    
    private fun patchGlobalPalette() {
        val clazz = classWrappers[GLOBAL_PALETTE_NAME]!!
        val instructions = clazz.getMethod("SRM(net.minecraft.world.level.chunk.GlobalPalette idFor)")!!.instructions
        val label = instructions[0] as LabelNode
        instructions.insert(buildInsnList {
            // if (param0 instanceof WrapperBlock) {
            //     param0 = ((WrapperBlock) param0).getDelegate();
            // }
            addLabel()
            aLoad(1)
            instanceOf(WRAPPER_INTERNAL_NAME)
            ifeq(label)
            addLabel()
            aLoad(1)
            checkCast(WRAPPER_INTERNAL_NAME)
            invokeVirtual(WRAPPER_INTERNAL_NAME, "getDelegate", "()LSRC/(net.minecraft.world.level.block.Block);")
            aStore(1)
        })
    }
    
}