package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block
import org.bukkit.Material
import org.bukkit.Tag
import xyz.xenondevs.nmsutils.adapter.NonNullAdapter
import xyz.xenondevs.nmsutils.advancement.AdvancementDsl
import xyz.xenondevs.nmsutils.util.nmsBlock
import xyz.xenondevs.nmsutils.util.tagKey
import net.minecraft.advancements.critereon.BlockPredicate as MojangBlockPredicate

class BlockPredicate(
    val tag: Tag<Material>?,
    val blocks: Set<Material>?,
    val properties: StatePropertiesPredicate?,
    val nbt: NbtPredicate?
) : Predicate {
    
    companion object : NonNullAdapter<BlockPredicate, MojangBlockPredicate>(MojangBlockPredicate.ANY) {
        
        @Suppress("UNCHECKED_CAST")
        override fun convert(value: BlockPredicate): MojangBlockPredicate {
            return MojangBlockPredicate(
                value.tag?.tagKey as TagKey<Block>?,
                value.blocks?.mapTo(HashSet(), Material::nmsBlock),
                StatePropertiesPredicate.toNMS(value.properties),
                NbtPredicate.toNMS(value.nbt)
            )
        }
        
    }
    
    @AdvancementDsl
    class Builder {
        
        private var tag: Tag<Material>? = null
        private val blocks = HashSet<Material>()
        private var properties: StatePropertiesPredicate? = null
        private var nbt: NbtPredicate? = null
        
        fun tag(tag: Tag<Material>) {
            this.tag = tag
        }
        
        fun materials(vararg blocks: Material) {
            this.blocks += blocks.toHashSet()
        }
        
        fun materials(blocks: Iterable<Material>) {
            this.blocks += blocks.toHashSet()
        }
        
        fun material(block: Material) {
            this.blocks += block
        }
        
        fun properties(init: StatePropertiesPredicate.Builder.() -> Unit) {
            this.properties = StatePropertiesPredicate.Builder().apply(init).build()
        }
        
        fun nbt(nbt: NbtPredicate) {
            this.nbt = nbt
        }
        
        fun nbt(nbt: String) {
            this.nbt = NbtPredicate(nbt)
        }
        
        internal fun build(): BlockPredicate {
            return BlockPredicate(
                tag,
                blocks.takeUnless(Collection<*>::isEmpty),
                properties,
                nbt
            )
        }
        
    }
    
}