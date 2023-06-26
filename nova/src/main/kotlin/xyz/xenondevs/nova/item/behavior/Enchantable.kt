package xyz.xenondevs.nova.item.behavior

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import xyz.xenondevs.commons.collections.isNotNullOrEmpty
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.enchantment.Enchantment
import xyz.xenondevs.nova.item.enchantment.NovaEnchantment
import xyz.xenondevs.nova.item.enchantment.VanillaEnchantment
import xyz.xenondevs.nova.item.options.EnchantableOptions
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.data.getOrNull
import xyz.xenondevs.nova.util.data.getOrPut
import xyz.xenondevs.nova.util.getOrThrow
import xyz.xenondevs.nova.util.item.novaCompound
import xyz.xenondevs.nova.util.item.novaCompoundOrNull
import xyz.xenondevs.nova.util.item.novaItem
import net.minecraft.world.item.ItemStack as MojangStack

class Enchantable(val options: EnchantableOptions) : ItemBehavior() {
    
    companion object : ItemBehaviorFactory<Enchantable>() {
        
        override fun create(item: NovaItem) =
            Enchantable(EnchantableOptions.configurable(item))
        
        // TODO: Bukkit methods
        
        @JvmStatic
        fun isEnchantable(itemStack: MojangStack): Boolean {
            val enchantable = itemStack.novaItem?.hasBehavior<Enchantable>() 
                ?: itemStack.item.isEnchantable(itemStack)
            return enchantable && !isEnchanted(itemStack)
        }
        
        @JvmStatic
        fun isEnchanted(itemStack: MojangStack): Boolean {
            if (itemStack.novaCompoundOrNull?.enchantmentsOrNull.isNotNullOrEmpty())
                return true
            return itemStack.tag?.getOrNull<ListTag>("Enchantments").isNotNullOrEmpty()
        }
        
        @JvmStatic
        fun getEnchantments(itemStack: MojangStack): MutableMap<Enchantment, Int> {
            val enchantments = HashMap<Enchantment, Int>()
            itemStack.tag?.getOrNull<ListTag>("Enchantments")?.asSequence()
                ?.filterIsInstance<CompoundTag>()
                ?.forEach {
                    val enchantment = NovaRegistries.ENCHANTMENT.getOrThrow(it.getString("id"))
                    val level = it.getShort("lvl").toInt()
                    enchantments[enchantment] = level
                }
            itemStack.novaCompoundOrNull?.enchantmentsOrNull?.let(enchantments::putAll)
            
            return enchantments
        }
        
        @JvmStatic
        fun setEnchantments(itemStack: MojangStack, enchantments: MutableMap<Enchantment, Int>) {
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
            
            itemStack.orCreateTag.put("Enchantments", vanillaEnchantmentsTag)
            itemStack.novaCompound["nova", "enchantments"] = novaEnchantmentsMap
        }
        
        @JvmStatic
        fun addEnchantment(itemStack: MojangStack, enchantment: Enchantment, level: Int) {
            if (enchantment is VanillaEnchantment) {
                val id = enchantment.id.toString()
                val enchantments = itemStack.orCreateTag.getOrPut("Enchantments", ::ListTag)
                enchantments.removeIf { it is CompoundTag && it.getOrNull<StringTag>("id")?.asString == id }
                enchantments += CompoundTag().apply { 
                    putString("id", id)
                    putShort("lvl", level.toShort())
                }
            } else if (enchantment is NovaEnchantment) {
                itemStack.novaCompound.enchantments[enchantment] = level
            }
        }
        
        @JvmStatic
        fun removeEnchantment(itemStack: MojangStack, enchantment: Enchantment) {
            if (enchantment is VanillaEnchantment) {
                val id = enchantment.id.toString()
                itemStack.tag?.getOrNull<ListTag>("Enchantments")
                    ?.removeIf { it is CompoundTag && it.getOrNull<StringTag>("id")?.asString == id }
            } else if (enchantment is NovaEnchantment) {
                itemStack.novaCompoundOrNull?.enchantmentsOrNull?.remove(enchantment)
            }
        }
        
        @JvmStatic
        fun removeAllEnchantments(itemStack: MojangStack) {
            itemStack.tag?.remove("Enchantments")
            itemStack.novaCompoundOrNull?.remove("nova", "enchantments")
        }
        
        private val NamespacedCompound.enchantmentsOrNull: MutableMap<Enchantment, Int>?
            get() = get("nova", "enchantments")
        
        private val NamespacedCompound.enchantments: MutableMap<Enchantment, Int>
            get() = getOrPut("nova", "enchantments", ::HashMap)
        
    }
    
}