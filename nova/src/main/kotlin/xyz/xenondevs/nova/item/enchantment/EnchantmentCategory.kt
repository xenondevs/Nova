package xyz.xenondevs.nova.item.enchantment

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import org.bukkit.Material
import org.bukkit.Tag
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.behavior.Enchantable
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.RegistryElementBuilder
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.bukkitMaterial
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.nmsItem
import net.minecraft.world.item.Item as MojangItem
import net.minecraft.world.item.ItemStack as MojangStack
import net.minecraft.world.item.enchantment.EnchantmentCategory as MojangEnchantmentCategory
import org.bukkit.inventory.ItemStack as BukkitStack

abstract class EnchantmentCategory {
    
    /**
     * The [id][ResourceLocation] of this [EnchantmentCategory].
     */
    abstract val id: ResourceLocation
    
    /**
     * The [Enchantments][Enchantment] in this category.
     */
    abstract val enchantments: MutableList<Enchantment>
    
    /**
     * Checks if this [EnchantmentCategory] can be applied to the specified [bukkitStack].
     */
    fun canEnchant(bukkitStack: BukkitStack): Boolean {
        val novaItem = bukkitStack.novaItem
        if (novaItem != null)
            return canEnchant(novaItem)
        
        return canEnchant(bukkitStack.type)
    }
    
    /**
     * Checks if this [EnchantmentCategory] can be applied to the specified [mojangStack].
     */
    fun canEnchant(mojangStack: MojangStack): Boolean {
        val novaItem = mojangStack.novaItem
        if (novaItem != null)
            return canEnchant(novaItem)
        
        return canEnchant(mojangStack.item)
    }
    
    /**
     * Checks if this [EnchantmentCategory] can be applied to the specified [novaItem].
     */
    fun canEnchant(novaItem: NovaItem): Boolean {
        val enchantable = novaItem.getBehaviorOrNull<Enchantable>()
        return enchantable?.enchantmentCategories?.contains(this) ?: false
    }
    
    /**
     * Checks if this [EnchantmentCategory] can be applied to the specified [item].
     */
    abstract fun canEnchant(item: MojangItem): Boolean
    
    /**
     * Checks if this [EnchantmentCategory] can be applied to the specified [item].
     */
    abstract fun canEnchant(item: Material): Boolean
    
}

internal class NovaEnchantmentCategory(
    override val id: ResourceLocation,
    private val parents: Collection<EnchantmentCategory>,
    private val vanillaItems: Set<MojangItem>
) : EnchantmentCategory() {
    
    override val enchantments = ArrayList<Enchantment>()
    private val vanillaMaterials = vanillaItems.mapTo(HashSet(), MojangItem::bukkitMaterial)
    
    override fun canEnchant(item: Item): Boolean =
        item in vanillaItems || parents.any { it.canEnchant(item) }
    
    override fun canEnchant(item: Material): Boolean =
        item in vanillaMaterials || parents.any { it.canEnchant(item) }
    
}

internal class VanillaEnchantmentCategory(
    override val id: ResourceLocation,
    private val vanillaCategory: MojangEnchantmentCategory
) : EnchantmentCategory() {
    
    override val enchantments = ArrayList<Enchantment>()
    
    init {
        for (ench in BuiltInRegistries.ENCHANTMENT) {
            if (ench.category == vanillaCategory)
                enchantments.add(Enchantment.of(ench))
        }
    }
    
    override fun canEnchant(item: Material): Boolean =
        vanillaCategory.canEnchant(item.nmsItem)
    
    override fun canEnchant(item: Item): Boolean =
        vanillaCategory.canEnchant(item)
    
}

class EnchantmentCategoryBuilder internal constructor(id: ResourceLocation) : RegistryElementBuilder<EnchantmentCategory>(NovaRegistries.ENCHANTMENT_CATEGORY, id) {
    
    private val parents = ArrayList<EnchantmentCategory>()
    private val vanillaItems = HashSet<MojangItem>()
    
    internal constructor(addon: Addon, name: String) : this(ResourceLocation(addon, name))
    
    /**
     * Configures from which [EnchantmentCategories][EnchantmentCategory] this [EnchantmentCategory] inherits.
     */
    fun inheritsFrom(vararg parents: EnchantmentCategory): EnchantmentCategoryBuilder {
        this.parents += parents
        return this
    }
    
    /**
     * Configures which vanilla items this [EnchantmentCategory] can enchant.
     */
    fun enchants(vararg vanillaItems: Material): EnchantmentCategoryBuilder {
        for (material in vanillaItems) this.vanillaItems += material.nmsItem
        return this
    }
    
    /**
     * Configures which vanilla items this [EnchantmentCategory] can enchant.
     */
    fun enchants(vararg vanillaTags: Tag<Material>): EnchantmentCategoryBuilder {
        for (tag in vanillaTags) {
            for (material in tag.values) {
                this.vanillaItems += material.nmsItem
            }
        }
        return this
    }
    
    /**
     * Configures which vanilla items this [EnchantmentCategory] can enchant.
     */
    fun enchants(vararg vanillaItems: MojangItem): EnchantmentCategoryBuilder {
        this.vanillaItems += vanillaItems
        return this
    }
    
    /**
     * Configures which vanilla items this [EnchantmentCategory] can enchant.
     */
    fun enchants(vararg vanillaTags: TagKey<MojangItem>): EnchantmentCategoryBuilder {
        for (tag in vanillaTags) {
            for (holder in BuiltInRegistries.ITEM.getTag(tag).get()) {
                this.vanillaItems += holder.value()
            }
        }
        return this
    }
    
    override fun build(): EnchantmentCategory = NovaEnchantmentCategory(id, parents, vanillaItems)
    
}