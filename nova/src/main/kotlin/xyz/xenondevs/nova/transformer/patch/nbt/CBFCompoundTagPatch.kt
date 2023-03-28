@file:Suppress("unused")

package xyz.xenondevs.nova.transformer.patch.nbt

import net.minecraft.CrashReport
import net.minecraft.ReportedException
import net.minecraft.nbt.ByteArrayTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.Tag
import net.minecraft.nbt.TagType
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.nova.data.serialization.cbf.CBFCompoundTagType
import xyz.xenondevs.nova.transformer.ClassTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import java.io.DataInput
import java.io.IOException

internal object CBFCompoundTagPatch : ClassTransformer(CompoundTag::class) {
    
    override fun transform() {
        transformReadNamedTagData()
    }
    
    private fun transformReadNamedTagData() {
        val method = VirtualClassPath[ReflectionRegistry.COMPOUND_TAG_READ_NAMED_TAG_DATA_METHOD]
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
    
}