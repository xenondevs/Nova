package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.advancements.critereon.BlockPredicate
import net.minecraft.advancements.critereon.NbtPredicate
import net.minecraft.advancements.critereon.StatePropertiesPredicate
import net.minecraft.nbt.CompoundTag
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block
import org.bukkit.Material
import xyz.xenondevs.nmsutils.internal.util.nmsBlock

class BlockPredicateBuilder : PredicateBuilder<BlockPredicate>() {
    
    private var tag: TagKey<Block>? = null
    private val blocks = HashSet<Block>()
    private var properties = StatePropertiesPredicate.ANY
    private var nbt = NbtPredicate.ANY
    
    fun tag(tag: TagKey<Block>) {
        this.tag = tag
    }
    
    fun blocks(vararg blocks: Block) {
        this.blocks += blocks
    }
    
    fun blocks(vararg blocks: Material) {
        this.blocks += blocks.map(Material::nmsBlock)
    }
    
    fun blocks(blocks: Iterable<Block>) {
        this.blocks += blocks
    }
    
    @JvmName("blocks1")
    fun blocks(blocks: Iterable<Material>) {
        this.blocks += blocks.map(Material::nmsBlock)
    }
    
    fun properties(init: StatePropertiesPredicateBuilder.() -> Unit) {
        this.properties = StatePropertiesPredicateBuilder().apply(init).build()
    }
    
    fun nbt(nbt: CompoundTag) {
        this.nbt = NbtPredicate(nbt)
    }
    
    fun nbt(init: CompoundTag.() -> Unit) {
        this.nbt = NbtPredicate(CompoundTag().apply(init))
    }
    
    override fun build(): BlockPredicate {
        return BlockPredicate(
            tag,
            blocks.takeUnless(Collection<*>::isEmpty),
            properties,
            nbt
        )
    }
    
}