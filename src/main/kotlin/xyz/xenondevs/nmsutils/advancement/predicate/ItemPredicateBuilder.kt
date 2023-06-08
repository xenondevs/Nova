package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.advancements.critereon.EnchantmentPredicate
import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.advancements.critereon.MinMaxBounds
import net.minecraft.advancements.critereon.NbtPredicate
import net.minecraft.nbt.CompoundTag
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.alchemy.Potion
import net.minecraft.world.item.enchantment.Enchantment
import org.bukkit.Material
import xyz.xenondevs.nmsutils.internal.util.nmsItem
import net.minecraft.advancements.critereon.EnchantmentPredicate as MojangEnchantmentPredicate

class ItemPredicateBuilder : PredicateBuilder<ItemPredicate>() {
    
    private var tag: TagKey<Item>? = null
    private var count = MinMaxBounds.Ints.ANY
    private var durability = MinMaxBounds.Ints.ANY
    private val items = HashSet<Item>()
    private val enchantments = ArrayList<MojangEnchantmentPredicate>()
    private val storedEnchantments = ArrayList<MojangEnchantmentPredicate>()
    private var potion: Potion? = null
    private var nbt = NbtPredicate.ANY
    
    fun tag(tag: TagKey<Item>) {
        this.tag = tag
    }
    
    fun items(vararg items: Item) {
        this.items += items
    }
    
    fun items(items: Iterable<Item>) {
        this.items += items
    }
    
    fun items(vararg material: Material) {
        items += material.map(Material::nmsItem)
    }
    
    @JvmName("items1")
    fun items(materials: Iterable<Material>) {
        this.items += materials.map(Material::nmsItem)
    }
    
    fun enchantment(init: EnchantmentPredicateBuilder.() -> Unit) {
        this.enchantments += EnchantmentPredicateBuilder().apply(init).build()
    }
    
    fun enchantment(enchantment: Enchantment) {
        enchantments += EnchantmentPredicate(enchantment, null)
    }
    
    fun storedEnchantment(init: EnchantmentPredicateBuilder.() -> Unit) {
        this.storedEnchantments += EnchantmentPredicateBuilder().apply(init).build()
    }
    
    fun storedEnchantment(enchantment: Enchantment) {
        storedEnchantments += MojangEnchantmentPredicate(enchantment, null)
    }
    
    fun count(count: MinMaxBounds.Ints) {
        this.count = count
    }
    
    fun count(count: IntRange) {
        this.count = MinMaxBounds.Ints.between(count.first, count.last)
    }
    
    fun count(count: Int) {
        this.count = MinMaxBounds.Ints.exactly(count)
    }
    
    fun durability(durability: MinMaxBounds.Ints) {
        this.durability = durability
    }
    
    fun durability(durability: IntRange) {
        this.durability = MinMaxBounds.Ints.between(durability.first, durability.last)
    }
    
    fun durability(durability: Int) {
        this.durability = MinMaxBounds.Ints.exactly(durability)
    }
    
    fun potion(potion: Potion) {
        this.potion = potion
    }
    
    fun nbt(nbt: CompoundTag) {
        this.nbt = NbtPredicate(nbt)
    }
    
    fun nbt(init: CompoundTag.() -> Unit) {
        this.nbt = NbtPredicate(CompoundTag().apply(init))
    }
    
    override fun build(): ItemPredicate {
        return ItemPredicate(
            tag,
            items.takeUnless(Set<*>::isEmpty),
            count,
            durability,
            enchantments.toTypedArray(),
            storedEnchantments.toTypedArray(),
            potion,
            nbt
        )
    }
    
}