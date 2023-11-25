package xyz.xenondevs.nova.item.behavior

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.world.item.Items
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import xyz.xenondevs.commons.collections.isNotNullOrEmpty
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.provider
import xyz.xenondevs.nova.data.config.entry
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.enchantment.Enchantment
import xyz.xenondevs.nova.item.enchantment.EnchantmentCategory
import xyz.xenondevs.nova.item.enchantment.NovaEnchantment
import xyz.xenondevs.nova.item.enchantment.VanillaEnchantment
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.data.getOrNull
import xyz.xenondevs.nova.util.data.getOrPut
import xyz.xenondevs.nova.util.get
import xyz.xenondevs.nova.util.item.novaCompound
import xyz.xenondevs.nova.util.item.novaCompoundOrNull
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.nmsCopy
import net.minecraft.world.item.ItemStack as MojangStack
import org.bukkit.enchantments.Enchantment as BukkitEnchantment
import org.bukkit.inventory.ItemStack as BukkitStack

private const val ENCHANTMENTS_CBF = "enchantments"
private const val ENCHANTMENTS_NBT = "Enchantments"
private const val STORED_ENCHANTMENTS_CBF = "stored_enchantments"
private const val STORED_ENCHANTMENTS_NBT = "StoredEnchantments"

fun Enchantable(
    enchantmentValue: Int,
    enchantmentCategories: List<EnchantmentCategory>
) = Enchantable.Default(provider(enchantmentValue), provider(enchantmentCategories))

/**
 * Allows items to be enchanted.
 */
interface Enchantable {
    
    /**
     * The enchantment value of this item.
     * Items with a higher enchantment value have a higher chance of getting more secondary enchantments
     * when enchanted in the enchanting table.
     *
     * As an example, these are the vanilla enchantment values depending on the material:
     *
     * * Wood: 15
     * * Stone: 5
     * * Iron: 14
     * * Diamond: 10
     * * Gold: 22
     * * Netherite: 15
     */
    val enchantmentValue: Int
    
    /**
     * A list of enchantment categories that can be applied to this item.
     */
    val enchantmentCategories: List<EnchantmentCategory>
    
    class Default(
        enchantmentValue: Provider<Int>,
        enchantmentCategories: Provider<List<EnchantmentCategory>>,
    ) : ItemBehavior, Enchantable {
        
        override val enchantmentValue by enchantmentValue
        override val enchantmentCategories by enchantmentCategories
        
    }
    
    companion object : ItemBehaviorFactory<Default> {
        
        override fun create(item: NovaItem): Default {
            val cfg = item.config
            return Default(
                cfg.entry("enchantment_value"),
                cfg.entry<List<EnchantmentCategory>>("enchantment_categories")
            )
        }
        
        // -- Bukkit ItemStack --
        
        fun isEnchantable(itemStack: BukkitStack): Boolean =
            isEnchantable(itemStack.nmsCopy)
        
        fun isEnchanted(itemStack: BukkitStack): Boolean =
            isEnchanted(itemStack.nmsCopy)
        
        fun isEnchantedBook(itemStack: BukkitStack): Boolean =
            isEnchantedBook(itemStack.nmsCopy)
        
        fun hasStoredEnchantments(itemStack: BukkitStack): Boolean =
            hasStoredEnchantments(itemStack.nmsCopy)
        
        fun getEnchantments(itemStack: BukkitStack): Map<Enchantment, Int> =
            getEnchantments(itemStack.nmsCopy)
        
        fun getStoredEnchantments(itemStack: BukkitStack): Map<Enchantment, Int> =
            getStoredEnchantments(itemStack.nmsCopy)
        
        fun getEnchantmentsOrStoredEnchantments(itemStack: BukkitStack): Map<Enchantment, Int> =
            getEnchantmentsOrStoredEnchantments(itemStack.nmsCopy)
        
        fun setEnchantments(itemStack: BukkitStack, enchantments: Map<Enchantment, Int>) {
            // clear vanilla enchants
            for ((enchantment, _) in itemStack.enchantments)
                itemStack.removeEnchantment(enchantment)
            
            setEnchantments(ENCHANTMENTS_CBF, itemStack::addUnsafeEnchantment, itemStack, enchantments)
        }
        
        fun setStoredEnchantments(itemStack: BukkitStack, enchantments: Map<Enchantment, Int>) {
            val meta = itemStack.itemMeta as? EnchantmentStorageMeta ?: return
            // clear vanilla enchants
            for ((enchantment, _) in meta.storedEnchants)
                meta.removeStoredEnchant(enchantment)
            
            setEnchantments(STORED_ENCHANTMENTS_CBF, { ench, lvl -> meta.addStoredEnchant(ench, lvl, true) }, itemStack, enchantments)
        }
        
        fun setEnchantmentsOrStoredEnchantments(itemStack: BukkitStack, enchantments: Map<Enchantment, Int>) {
            if (isEnchantedBook(itemStack)) {
                setStoredEnchantments(itemStack, enchantments)
            } else {
                setEnchantments(itemStack, enchantments)
            }
        }
        
        private fun setEnchantments(cbfName: String, addVanillaEnchantment: (BukkitEnchantment, Int) -> Unit, itemStack: BukkitStack, enchantments: Map<Enchantment, Int>) {
            val novaEnchantmentsMap = HashMap<Enchantment, Int>()
            for ((enchantment, level) in enchantments) {
                if (enchantment is VanillaEnchantment) {
                    addVanillaEnchantment(Enchantment.asBukkitEnchantment(enchantment), level)
                } else {
                    novaEnchantmentsMap[enchantment] = level
                }
            }
            
            if (novaEnchantmentsMap.isNotEmpty()) {
                itemStack.novaCompound["nova", cbfName] = novaEnchantmentsMap
            } else {
                itemStack.novaCompoundOrNull?.remove("nova", cbfName)
            }
        }
        
        fun addEnchantment(itemStack: BukkitStack, enchantment: Enchantment, level: Int) =
            addEnchantment(ENCHANTMENTS_CBF, itemStack::addUnsafeEnchantment, itemStack, enchantment, level)
        
        fun addStoredEnchantment(itemStack: BukkitStack, enchantment: Enchantment, level: Int) {
            val meta = itemStack.itemMeta as? EnchantmentStorageMeta ?: return
            addEnchantment(STORED_ENCHANTMENTS_CBF, { ench, lvl -> meta.addStoredEnchant(ench, lvl, true) }, itemStack, enchantment, level)
        }
        
        fun addEnchantmentOrStoredEnchantment(itemStack: BukkitStack, enchantment: Enchantment, level: Int) {
            if (isEnchantedBook(itemStack)) {
                addStoredEnchantment(itemStack, enchantment, level)
            } else {
                addEnchantment(itemStack, enchantment, level)
            }
        }
        
        private fun addEnchantment(cbfName: String, addVanillaEnchantment: (BukkitEnchantment, Int) -> Unit, itemStack: BukkitStack, enchantment: Enchantment, level: Int) {
            if (enchantment is VanillaEnchantment) {
                addVanillaEnchantment(Enchantment.asBukkitEnchantment(enchantment), level)
            } else {
                itemStack.novaCompound.getEnchantments(cbfName)[enchantment] = level
            }
        }
        
        fun removeEnchantment(itemStack: BukkitStack, enchantment: Enchantment) =
            removeEnchantment(ENCHANTMENTS_CBF, itemStack::removeEnchantment, itemStack, enchantment)
        
        fun removeStoredEnchantment(itemStack: BukkitStack, enchantment: Enchantment) {
            val meta = itemStack.itemMeta as? EnchantmentStorageMeta ?: return
            removeEnchantment(STORED_ENCHANTMENTS_CBF, meta::removeStoredEnchant, itemStack, enchantment)
        }
        
        fun removeEnchantmentOrStoredEnchantment(itemStack: BukkitStack, enchantment: Enchantment) {
            if (isEnchantedBook(itemStack)) {
                removeStoredEnchantment(itemStack, enchantment)
            } else {
                removeEnchantment(itemStack, enchantment)
            }
        }
        
        private fun removeEnchantment(cbfName: String, removeVanillaEnchantment: (BukkitEnchantment) -> Unit, itemStack: BukkitStack, enchantment: Enchantment) {
            if (enchantment is VanillaEnchantment) {
                removeVanillaEnchantment(Enchantment.asBukkitEnchantment(enchantment))
            } else {
                itemStack.novaCompoundOrNull?.getEnchantmentsOrNull(cbfName)?.remove(enchantment)
            }
        }
        
        fun removeAllEnchantments(itemStack: BukkitStack) {
            for ((enchantment, _) in itemStack.enchantments)
                itemStack.removeEnchantment(enchantment)
            itemStack.novaCompoundOrNull?.remove("nova", ENCHANTMENTS_CBF)
        }
        
        fun removeAllStoredEnchantments(itemStack: BukkitStack) {
            val meta = itemStack.itemMeta as? EnchantmentStorageMeta ?: return
            for ((enchantment, _) in meta.enchants)
                meta.removeStoredEnchant(enchantment)
            itemStack.novaCompoundOrNull?.remove("nova", STORED_ENCHANTMENTS_CBF)
        }
        
        fun removeAllEnchantmentsOrStoredEnchantments(itemStack: BukkitStack) {
            if (isEnchantedBook(itemStack)) {
                removeAllStoredEnchantments(itemStack)
            } else {
                removeAllEnchantments(itemStack)
            }
        }
        
        // -- Mojang ItemStack --
        
        @JvmStatic
        fun isEnchantable(itemStack: MojangStack): Boolean {
            val enchantable = itemStack.novaItem?.hasBehavior<Enchantable>()
                ?: itemStack.item.isEnchantable(itemStack)
            return enchantable && !isEnchanted(itemStack)
        }
        
        @JvmStatic
        fun isEnchanted(itemStack: MojangStack): Boolean =
            isEnchanted(ENCHANTMENTS_CBF, ENCHANTMENTS_NBT, itemStack)
        
        fun isEnchantedBook(itemStack: MojangStack): Boolean =
            itemStack.novaItem == null && itemStack.item == Items.ENCHANTED_BOOK
        
        fun hasStoredEnchantments(itemStack: MojangStack): Boolean =
            isEnchanted(STORED_ENCHANTMENTS_CBF, STORED_ENCHANTMENTS_NBT, itemStack)
        
        private fun isEnchanted(cbfName: String, nbtName: String, itemStack: MojangStack): Boolean {
            if (itemStack.novaCompoundOrNull?.getEnchantmentsOrNull(cbfName).isNotNullOrEmpty())
                return true
            return itemStack.tag?.getOrNull<ListTag>(nbtName).isNotNullOrEmpty()
        }
        
        fun getEnchantments(itemStack: MojangStack): Map<Enchantment, Int> =
            getEnchantments(ENCHANTMENTS_CBF, ENCHANTMENTS_NBT, itemStack)
        
        fun getStoredEnchantments(itemStack: MojangStack): Map<Enchantment, Int> =
            getEnchantments(STORED_ENCHANTMENTS_CBF, STORED_ENCHANTMENTS_NBT, itemStack)
        
        fun getEnchantmentsOrStoredEnchantments(itemStack: MojangStack): Map<Enchantment, Int> {
            if (isEnchantedBook(itemStack))
                return getStoredEnchantments(itemStack)
            return getEnchantments(itemStack)
        }
        
        private fun getEnchantments(cbfName: String, nbtName: String, itemStack: MojangStack): Map<Enchantment, Int> {
            val enchantments = HashMap<Enchantment, Int>()
            itemStack.tag?.getOrNull<ListTag>(nbtName)?.asSequence()
                ?.filterIsInstance<CompoundTag>()
                ?.forEach {
                    val enchantment = NovaRegistries.ENCHANTMENT[it.getString("id")] ?: return@forEach
                    val level = it.getShort("lvl").toInt()
                    enchantments[enchantment] = level
                }
            itemStack.novaCompoundOrNull?.getEnchantmentsOrNull(cbfName)?.let(enchantments::putAll)
            
            return enchantments
        }
        
        fun setEnchantments(itemStack: MojangStack, enchantments: Map<Enchantment, Int>) =
            setEnchantments(ENCHANTMENTS_CBF, ENCHANTMENTS_NBT, itemStack, enchantments)
        
        fun setStoredEnchantments(itemStack: MojangStack, enchantments: Map<Enchantment, Int>) =
            setEnchantments(STORED_ENCHANTMENTS_CBF, STORED_ENCHANTMENTS_NBT, itemStack, enchantments)
        
        fun setEnchantmentsOrStoredEnchantments(itemStack: MojangStack, enchantments: Map<Enchantment, Int>) {
            if (isEnchantedBook(itemStack)) {
                setStoredEnchantments(itemStack, enchantments)
            } else {
                setEnchantments(itemStack, enchantments)
            }
        }
        
        private fun setEnchantments(cbfName: String, nbtName: String, itemStack: MojangStack, enchantments: Map<Enchantment, Int>) {
            val vanillaEnchantmentsTag = ListTag()
            val novaEnchantmentsMap = HashMap<Enchantment, Int>()
            
            for ((enchantment, level) in enchantments) {
                if (enchantment is VanillaEnchantment) {
                    val compound = CompoundTag().apply {
                        putString("id", enchantment.id.toString())
                        putShort("lvl", level.toShort())
                    }
                    vanillaEnchantmentsTag += compound
                } else if (enchantment is NovaEnchantment) {
                    novaEnchantmentsMap[enchantment] = level
                }
            }
            
            if (vanillaEnchantmentsTag.isNotEmpty()) {
                itemStack.orCreateTag.put(nbtName, vanillaEnchantmentsTag)
            } else {
                itemStack.tag?.remove(nbtName)
            }
            
            if (novaEnchantmentsMap.isNotEmpty()) {
                itemStack.novaCompound["nova", cbfName] = novaEnchantmentsMap
            } else {
                itemStack.novaCompoundOrNull?.remove("nova", cbfName)
            }
        }
        
        fun addEnchantment(itemStack: MojangStack, enchantment: Enchantment, level: Int) =
            addEnchantment(ENCHANTMENTS_CBF, ENCHANTMENTS_NBT, itemStack, enchantment, level)
        
        fun addStoredEnchantment(itemStack: MojangStack, enchantment: Enchantment, level: Int) =
            addEnchantment(STORED_ENCHANTMENTS_CBF, STORED_ENCHANTMENTS_NBT, itemStack, enchantment, level)
        
        fun addEnchantmentOrStoredEnchantment(itemStack: MojangStack, enchantment: Enchantment, level: Int) {
            if (isEnchantedBook(itemStack)) {
                addStoredEnchantment(itemStack, enchantment, level)
            } else {
                addEnchantment(itemStack, enchantment, level)
            }
        }
        
        private fun addEnchantment(cbfName: String, nbtName: String, itemStack: MojangStack, enchantment: Enchantment, level: Int) {
            if (enchantment is VanillaEnchantment) {
                val id = enchantment.id.toString()
                val enchantments = itemStack.orCreateTag.getOrPut(nbtName, ::ListTag)
                enchantments.removeIf { it is CompoundTag && it.getOrNull<StringTag>("id")?.asString == id }
                enchantments += CompoundTag().apply {
                    putString("id", id)
                    putShort("lvl", level.toShort())
                }
            } else if (enchantment is NovaEnchantment) {
                itemStack.novaCompound.getEnchantments(cbfName)[enchantment] = level
            }
        }
        
        fun removeEnchantment(itemStack: MojangStack, enchantment: Enchantment) =
            removeEnchantment(ENCHANTMENTS_CBF, ENCHANTMENTS_NBT, itemStack, enchantment)
        
        fun removeStoredEnchantment(itemStack: MojangStack, enchantment: Enchantment) =
            removeEnchantment(STORED_ENCHANTMENTS_CBF, STORED_ENCHANTMENTS_NBT, itemStack, enchantment)
        
        fun removeEnchantmentOrStoredEnchantment(itemStack: MojangStack, enchantment: Enchantment) {
            if (isEnchantedBook(itemStack)) {
                removeStoredEnchantment(itemStack, enchantment)
            } else {
                removeEnchantment(itemStack, enchantment)
            }
        }
        
        private fun removeEnchantment(cbfName: String, nbtName: String, itemStack: MojangStack, enchantment: Enchantment) {
            if (enchantment is VanillaEnchantment) {
                val id = enchantment.id.toString()
                itemStack.tag?.getOrNull<ListTag>(nbtName)
                    ?.removeIf { it is CompoundTag && it.getOrNull<StringTag>("id")?.asString == id }
            } else if (enchantment is NovaEnchantment) {
                itemStack.novaCompoundOrNull?.getEnchantmentsOrNull(cbfName)?.remove(enchantment)
            }
        }
        
        fun removeAllEnchantments(itemStack: MojangStack) =
            removeAllEnchantments(ENCHANTMENTS_CBF, ENCHANTMENTS_NBT, itemStack)
        
        fun removeAllStoredEnchantments(itemStack: MojangStack) =
            removeAllEnchantments(STORED_ENCHANTMENTS_CBF, STORED_ENCHANTMENTS_NBT, itemStack)
        
        fun removeAllEnchantmentsOrStoredEnchantments(itemStack: MojangStack) {
            if (isEnchantedBook(itemStack)) {
                removeAllStoredEnchantments(itemStack)
            } else {
                removeAllEnchantments(itemStack)
            }
        }
        
        private fun removeAllEnchantments(cbfName: String, nbtName: String, itemStack: MojangStack) {
            itemStack.tag?.remove(nbtName)
            itemStack.novaCompoundOrNull?.remove("nova", cbfName)
        }
        
        // -- Misc --
        
        private fun NamespacedCompound.getEnchantmentsOrNull(name: String): MutableMap<Enchantment, Int>? =
            get("nova", name)
        
        private fun NamespacedCompound.getEnchantments(name: String): MutableMap<Enchantment, Int> =
            getOrPut("nova", name, ::HashMap)
        
    }
    
}