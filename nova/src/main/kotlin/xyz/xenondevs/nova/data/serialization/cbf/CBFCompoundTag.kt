package xyz.xenondevs.nova.data.serialization.cbf

import net.minecraft.nbt.ByteArrayTag
import net.minecraft.nbt.ByteTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.StreamTagVisitor
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.nbt.TagType
import net.minecraft.nbt.TagVisitor
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.io.ByteReader
import java.io.DataInput
import java.io.DataOutput
import java.util.*
import java.util.function.Consumer
import java.util.function.IntFunction
import java.util.function.Predicate
import java.util.function.UnaryOperator
import java.util.stream.Stream

internal object CBFCompoundTagType : TagType.VariableSize<ByteArrayTag> {
    
    override fun load(input: DataInput, i: Int, accounter: NbtAccounter?): CBFCompoundTag {
        val reader = ByteReader.fromDataInput(input)
        reader.skip(4) // skip length int
        return CBFCompoundTag(CBF.read(reader)!!)
    }
    
    override fun skip(input: DataInput){
        val reader = ByteReader.fromDataInput(input)
        reader.skip(4) // skip length int
        if (reader.readBoolean()) {
            reader.skip(reader.readVarInt())
        }
    }
    
    override fun getName(): String {
        return "CBFCompound"
    }
    
    override fun getPrettyName(): String {
        return "TAG_CBF_COMPOUND"
    }
    
    override fun parse(input: DataInput, visitor: StreamTagVisitor): StreamTagVisitor.ValueResult {
        throw UnsupportedOperationException()
    }
    
}

@Suppress("OVERRIDE_DEPRECATION")
internal class CBFCompoundTag(val compound: NamespacedCompound) : ByteArrayTag(byteArrayOf()) {
    
    constructor() : this(NamespacedCompound())
    
    override fun getId(): Byte = 7 // byte array
    override fun getType(): TagType<ByteArrayTag> = CBFCompoundTagType
    override fun copy(): Tag = CBFCompoundTag(compound.copy())
    override fun getAsByteArray(): ByteArray = CBF.write(compound)
    override fun accept(visitor: StreamTagVisitor): StreamTagVisitor.ValueResult = visitor.visit(compound.toString())
    override fun accept(visitor: TagVisitor) = visitor.visitString(StringTag.valueOf(compound.toString())) // TODO
    override fun acceptAsRoot(visitor: StreamTagVisitor) = throw UnsupportedOperationException() // TODO
    
    override fun write(out: DataOutput) {
        val bytes = CBF.write(compound)
        out.writeInt(bytes.size)
        out.write(bytes)
    }
    
    override fun equals(other: Any?): Boolean {
        if (other === this)
            return true
        
        return other is CBFCompoundTag && asByteArray.contentEquals(other.asByteArray)
    }
    
    override fun hashCode(): Int = asByteArray.contentHashCode()
    
    //<editor-fold desc="unsupported methods", defaultstate="collapsed">
    override val size: Int
        get() = throw UnsupportedOperationException()
    
    override fun sizeInBytes(): Int {
        throw UnsupportedOperationException()
    }
    
    override fun get(index: Int): ByteTag {
        throw UnsupportedOperationException()
    }
    
    override fun set(index: Int, element: ByteTag): ByteTag {
        throw UnsupportedOperationException()
    }
    
    override fun add(index: Int, element: ByteTag) {
        throw UnsupportedOperationException()
    }
    
    override fun add(element: ByteTag?): Boolean {
        throw UnsupportedOperationException()
    }
    
    override fun setTag(index: Int, element: Tag): Boolean {
        throw UnsupportedOperationException()
    }
    
    override fun addTag(index: Int, element: Tag): Boolean {
        throw UnsupportedOperationException()
    }
    
    override fun remove(element: ByteTag?): Boolean {
        throw UnsupportedOperationException()
    }
    
    override fun addAll(index: Int, elements: Collection<ByteTag>): Boolean {
        throw UnsupportedOperationException()
    }
    
    override fun addAll(elements: Collection<ByteTag>): Boolean {
        throw UnsupportedOperationException()
    }
    
    override fun clear() {
        throw UnsupportedOperationException()
    }
    
    override fun iterator(): MutableIterator<ByteTag> {
        throw UnsupportedOperationException()
    }
    
    override fun removeAll(elements: Collection<ByteTag>): Boolean {
        throw UnsupportedOperationException()
    }
    
    override fun retainAll(elements: Collection<ByteTag>): Boolean {
        throw UnsupportedOperationException()
    }
    
    override fun contains(element: ByteTag?): Boolean {
        throw UnsupportedOperationException()
    }
    
    override fun containsAll(elements: Collection<ByteTag>): Boolean {
        throw UnsupportedOperationException()
    }
    
    override fun isEmpty(): Boolean {
        throw UnsupportedOperationException()
    }
    
    override fun forEach(action: Consumer<in ByteTag>?) {
        throw UnsupportedOperationException()
    }
    
    override fun spliterator(): Spliterator<ByteTag> {
        throw UnsupportedOperationException()
    }
    
    override fun toArray(): Array<Any> {
        throw UnsupportedOperationException()
    }
    
    override fun <T : Any?> toArray(a: Array<out T>): Array<T> {
        throw UnsupportedOperationException()
    }
    
    override fun <T : Any?> toArray(generator: IntFunction<Array<T>>?): Array<T> {
        throw UnsupportedOperationException()
    }
    
    override fun removeIf(filter: Predicate<in ByteTag>): Boolean {
        throw UnsupportedOperationException()
    }
    
    override fun stream(): Stream<ByteTag> {
        throw UnsupportedOperationException()
    }
    
    override fun parallelStream(): Stream<ByteTag> {
        throw UnsupportedOperationException()
    }
    
    override fun listIterator(): MutableListIterator<ByteTag> {
        throw UnsupportedOperationException()
    }
    
    override fun listIterator(index: Int): MutableListIterator<ByteTag> {
        throw UnsupportedOperationException()
    }
    
    override fun removeAt(i: Int): ByteTag {
        throw UnsupportedOperationException()
    }
    
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<ByteTag> {
        throw UnsupportedOperationException()
    }
    
    override fun indexOf(element: ByteTag?): Int {
        throw UnsupportedOperationException()
    }
    
    override fun lastIndexOf(element: ByteTag?): Int {
        throw UnsupportedOperationException()
    }
    
    override fun replaceAll(operator: UnaryOperator<ByteTag>) {
        throw UnsupportedOperationException()
    }
    
    override fun sort(c: Comparator<in ByteTag>?) {
        throw UnsupportedOperationException()
    }
    
    override fun removeRange(fromIndex: Int, toIndex: Int) {
        throw UnsupportedOperationException()
    }
    
    override fun getElementType(): Byte {
        throw UnsupportedOperationException()
    }
    //</editor-fold>
    
}