package xyz.xenondevs.nova.patch.impl.registry

import com.google.common.collect.Sets
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.tags.TagKey
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.FieldInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.gets
import xyz.xenondevs.bytebase.util.insertAfterFirst
import xyz.xenondevs.bytebase.util.replaceFirst
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionUtils

private val HOLDER_REFERENCE_TAGS_FIELD = ReflectionUtils.getField(Holder.Reference::class, "tags")
private val NAMED_HOLDER_SET_CONTENTS_METHOD = ReflectionUtils.getMethod(HolderSet.Named::class, "contents")
private val HOLDER_REFERENCE_IS_TAG_KEY = ReflectionUtils.getMethod(Holder.Reference::class, "is", TagKey::class)

internal object TagsPatch : MultiTransformer(Holder.Reference::class, HolderSet.Named::class) {
    
    private val keyToExtraEntries = HashMap<TagKey<*>, HashSet<Holder<Any>>>()
    private val extraEntryToKeys = HashMap<Holder<Any>, HashSet<TagKey<Any>>>()
    
    @Suppress("UNCHECKED_CAST")
    fun addExtra(tagKey: TagKey<*>, holder: Holder<*>) {
        keyToExtraEntries.computeIfAbsent(tagKey) { HashSet() }.add(holder as Holder<Any>)
        extraEntryToKeys.computeIfAbsent(holder) { HashSet() }.add(tagKey as TagKey<Any>)
    }
    
    override fun transform() {
        VirtualClassPath[HOLDER_REFERENCE_IS_TAG_KEY].insertAfterFirst(buildInsnList {
            aLoad(0)
            invokeStatic(::modifiedTags)
        }) { it.opcode == Opcodes.GETFIELD && (it as FieldInsnNode).gets(HOLDER_REFERENCE_TAGS_FIELD) }
        
        VirtualClassPath[NAMED_HOLDER_SET_CONTENTS_METHOD].replaceFirst(
            0, 0,
            buildInsnList {
                aLoad(0)
                invokeVirtual(HolderSet.Named<*>::key)
                invokeStatic(::modifiedContents)
                areturn()
            }) { it.opcode == Opcodes.ARETURN }
    }
    
    @JvmStatic
    fun modifiedContents(contents: List<Holder<*>>, tagKey: TagKey<*>): List<*> {
        val mutableContents = contents.toMutableList()
        keyToExtraEntries[tagKey]?.let { mutableContents.addAll(it) }
        return mutableContents
    }
    
    @JvmStatic
    fun modifiedTags(tags: Set<TagKey<*>>, holder: Holder<*>): Set<TagKey<*>> {
        val extraTags = extraEntryToKeys[holder] ?: return tags
        return Sets.union(tags, extraTags)
    }
    
}