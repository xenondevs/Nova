package xyz.xenondevs.nmsutils.advancement.predicate

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.craftbukkit.v1_19_R1.util.CraftMagicNumbers
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import xyz.xenondevs.nmsutils.adapter.NonNullAdapter
import xyz.xenondevs.nmsutils.adapter.impl.IntBoundsAdapter
import xyz.xenondevs.nmsutils.advancement.AdvancementDsl
import xyz.xenondevs.nmsutils.util.mapToArray
import xyz.xenondevs.nmsutils.util.potion
import net.minecraft.advancements.critereon.EnchantmentPredicate as MojangEnchantmentPredicate
import net.minecraft.advancements.critereon.ItemPredicate as MojangItemPredicate

class ItemPredicate(
    val tag: Tag<Material>?,
    val types: List<Material>?,
    val enchantments: List<EnchantmentPredicate>?,
    val storedEnchantments: List<EnchantmentPredicate>?,
    val count: IntRange?,
    val durability: IntRange?,
    val potion: PotionEffect?,
    val nbt: NbtPredicate?
) : Predicate {
    
    companion object : NonNullAdapter<ItemPredicate, MojangItemPredicate>(MojangItemPredicate.ANY) {
        
        override fun convert(value: ItemPredicate): MojangItemPredicate {
            return MojangItemPredicate(
                null,
                value.types?.mapTo(HashSet()) { CraftMagicNumbers.getItem(it) }?.takeUnless(Collection<*>::isEmpty),
                IntBoundsAdapter.toNMS(value.count),
                IntBoundsAdapter.toNMS(value.durability),
                value.enchantments?.mapToArray(EnchantmentPredicate::toNMS) ?: MojangEnchantmentPredicate.NONE,
                value.storedEnchantments?.mapToArray(EnchantmentPredicate::toNMS) ?: MojangEnchantmentPredicate.NONE,
                value.potion?.let(PotionEffect::potion),
                NbtPredicate.toNMS(value.nbt)
            )
        }
        
    }
    
    @AdvancementDsl
    class Builder {
        
        private val types = ArrayList<Material>()
        private val enchantments = ArrayList<EnchantmentPredicate>()
        private val storedEnchantments = ArrayList<EnchantmentPredicate>()
        private var tag: Tag<Material>? = null
        private var count: IntRange? = null
        private var durability: IntRange? = null
        private var potion: PotionEffect? = null
        private var nbt: NbtPredicate? = null
        
        fun material(material: Material) {
            types += material
        }
        
        fun materials(materials: Iterable<Material>) {
            this.types += materials.toMutableList()
        }
        
        fun enchantment(init: EnchantmentPredicate.Builder.() -> Unit) {
            this.enchantments += EnchantmentPredicate.Builder().apply(init).build()
        }
        
        fun enchantment(enchantment: Enchantment) {
            enchantments += EnchantmentPredicate(enchantment, null)
        }
        
        fun storedEnchantment(init: EnchantmentPredicate.Builder.() -> Unit) {
            this.storedEnchantments += EnchantmentPredicate.Builder().apply(init).build()
        }
        
        fun storedEnchantment(enchantment: Enchantment) {
            storedEnchantments += EnchantmentPredicate(enchantment, null)
        }
        
        fun count(count: IntRange) {
            this.count = count
        }
        
        fun durability(durability: IntRange) {
            this.durability = durability
        }
        
        fun potion(potion: PotionEffect) {
            this.potion = potion
        }
        
        fun nbt(nbt: String) {
            this.nbt = NbtPredicate(nbt)
        }
        
        fun nbt(item: ItemStack) {
            nbt = NbtPredicate(item)
        }
        
        fun nbt(predicate: NbtPredicate) {
            nbt = predicate
        }
        
        internal fun build(): ItemPredicate {
            return ItemPredicate(
                tag,
                types.takeUnless(List<*>::isEmpty),
                enchantments.takeUnless(List<*>::isEmpty),
                storedEnchantments.takeUnless(List<*>::isEmpty),
                count,
                durability,
                potion,
                nbt
            )
        }
        
    }
    
}