@file:Suppress("unused")

package xyz.xenondevs.nova.transformer.patch.nbt

import net.minecraft.CrashReport
import net.minecraft.ReportedException
import net.minecraft.nbt.ByteArrayTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.Tag
import net.minecraft.nbt.TagType
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.gets
import xyz.xenondevs.bytebase.util.insertAfterFirst
import xyz.xenondevs.bytebase.util.isClass
import xyz.xenondevs.bytebase.util.replaceFirstAfter
import xyz.xenondevs.nova.data.serialization.cbf.CBFCompoundTagType
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.CB_CRAFT_META_APPLY_TO_ITEM_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.CB_CRAFT_META_ITEM_CLASS
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.COMPOUND_TAG_READ_NAMED_TAG_DATA_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.CRAFT_META_ITEM_CLONE_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.CRAFT_META_ITEM_UNHANDLED_TAGS_FIELD
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import java.io.DataInput
import java.io.IOException

internal object CBFCompoundTagPatch : MultiTransformer(CompoundTag::class, CB_CRAFT_META_ITEM_CLASS.kotlin) {
    
    override fun transform() {
        transformReadNamedTagData()
        
        // patch CraftMetaItem to copy unhandledTags
        transformCraftMetaItemApplyToItem()
        transformCraftMetaItemClone()
    }
    
    private fun transformReadNamedTagData() {
        val method = VirtualClassPath[COMPOUND_TAG_READ_NAMED_TAG_DATA_METHOD]
        method.localVariables.clear()
        method.tryCatchBlocks.clear()
        
        method.instructions = buildInsnList {
            aLoad(0)
            aLoad(1)
            aLoad(2)
            iLoad(3)
            aLoad(4)
            invokeStatic(::readNamedTagData)
            areturn()
        }
    }
    
    @JvmStatic
    fun readNamedTagData(type: TagType<*>, name: String, input: DataInput, depth: Int, accounter: NbtAccounter): Tag {
        try {
            if (type == ByteArrayTag.TYPE && name.endsWith("_cbf"))
                return CBFCompoundTagType.load(input, depth, accounter)
            
            return type.load(input, depth, accounter)
        } catch (e: IOException) {
            val report = CrashReport.forThrowable(e, "Loading NBT data (Modified by Nova)")
            val category = report.addCategory("NBT Tag")
            category.setDetail("Tag name", name)
            category.setDetail("Tag type", type.name)
            throw ReportedException(report)
        }
    }
    
    private fun transformCraftMetaItemApplyToItem() {
        val method = VirtualClassPath[CB_CRAFT_META_APPLY_TO_ITEM_METHOD]
        method.replaceFirstAfter(0, -1, buildInsnList { // insertFirstAfter
            invokeInterface(Tag::copy)
        },
            { it is FieldInsnNode && it.gets(CRAFT_META_ITEM_UNHANDLED_TAGS_FIELD) },
            match = { it is MethodInsnNode && it.calls(CompoundTag::put) }
        )
    }
    
    private fun transformCraftMetaItemClone() {
        val method = VirtualClassPath[CRAFT_META_ITEM_CLONE_METHOD]
        method.insertAfterFirst(buildInsnList {
            // CBFCompoundTagPatch.copyUnhandledTagsTo(this.unhandledTags, clone)
            
            aLoad(0) // this
            getField(CRAFT_META_ITEM_UNHANDLED_TAGS_FIELD)
            aLoad(1) // copy
            invokeStatic(::copyUnhandledTagsTo)
        }) { it.opcode == Opcodes.ASTORE && it.previous.opcode == Opcodes.CHECKCAST && (it.previous as TypeInsnNode).isClass(CB_CRAFT_META_ITEM_CLASS) }
    }
    
    @JvmStatic
    fun copyUnhandledTagsTo(unhandledTags: Map<String, Tag>, destinationItemMeta: Any) {
        val copy = unhandledTags.mapValuesTo(HashMap()) { it.value.copy() }
        ReflectionUtils.setFinalField(CRAFT_META_ITEM_UNHANDLED_TAGS_FIELD, destinationItemMeta, copy)
    }
    
}