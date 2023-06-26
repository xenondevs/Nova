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
import xyz.xenondevs.nova.util.get
import xyz.xenondevs.nova.util.item.novaCompound
import xyz.xenondevs.nova.util.item.novaCompoundOrNull
import xyz.xenondevs.nova.util.item.novaItem
import net.minecraft.world.item.ItemStack as MojangStack

private const val ENCHANTMENTS_CBF = "enchantments"
private const val ENCHANTMENTS_NBT = "Enchantments"
private const val STORED_ENCHANTMENTS_CBF = "stored_enchantments"
private const val STORED_ENCHANTMENTS_NBT = "StoredEnchantments"

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
        fun isEnchanted(itemStack: MojangStack): Boolean =
            isEnchanted(ENCHANTMENTS_CBF, ENCHANTMENTS_NBT, itemStack)
        
        fun hasStoredEnchantments(itemStack: MojangStack): Boolean =
            isEnchanted(STORED_ENCHANTMENTS_CBF, STORED_ENCHANTMENTS_NBT, itemStack)
        
        private fun isEnchanted(cbfName: String, nbtName: String, itemStack: MojangStack): Boolean {
            if (itemStack.novaCompoundOrNull?.getEnchantmentsOrNull(cbfName).isNotNullOrEmpty())
                return true
            return itemStack.tag?.getOrNull<ListTag>(nbtName).isNotNullOrEmpty()
        }
        
        fun getEnchantments(itemStack: MojangStack): MutableMap<Enchantment, Int> =
            getEnchantments(ENCHANTMENTS_CBF, ENCHANTMENTS_NBT, itemStack)
        
        fun getStoredEnchantments(itemStack: MojangStack): MutableMap<Enchantment, Int> =
            getEnchantments(STORED_ENCHANTMENTS_CBF, STORED_ENCHANTMENTS_NBT, itemStack)
        
        private fun getEnchantments(cbfName: String, nbtName: String, itemStack: MojangStack): MutableMap<Enchantment, Int> {
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
        
        fun setEnchantments(itemStack: MojangStack, enchantments: MutableMap<Enchantment, Int>) =
            setEnchantments(ENCHANTMENTS_CBF, ENCHANTMENTS_NBT, itemStack, enchantments)
        
        fun setStoredEnchantments(itemStack: MojangStack, enchantments: MutableMap<Enchantment, Int>) =
            setEnchantments(STORED_ENCHANTMENTS_CBF, STORED_ENCHANTMENTS_NBT, itemStack, enchantments)
        
        private fun setEnchantments(cbfName: String, nbtName: String, itemStack: MojangStack, enchantments: MutableMap<Enchantment, Int>) {
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
            
            if (vanillaEnchantmentsTag.isNotEmpty())
                itemStack.orCreateTag.put(nbtName, vanillaEnchantmentsTag)
            
            if (novaEnchantmentsMap.isNotEmpty())
                itemStack.novaCompound["nova", cbfName] = novaEnchantmentsMap
        }
        
        fun addEnchantment(itemStack: MojangStack, enchantment: Enchantment, level: Int) =
            addEnchantment(ENCHANTMENTS_CBF, ENCHANTMENTS_NBT, itemStack, enchantment, level)
        
        fun addStoredEnchantment(itemStack: MojangStack, enchantment: Enchantment, level: Int) =
            addEnchantment(STORED_ENCHANTMENTS_CBF, STORED_ENCHANTMENTS_NBT, itemStack, enchantment, level)
        
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
        
        private fun removeAllEnchantments(cbfName: String, nbtName: String, itemStack: MojangStack) {
            itemStack.tag?.remove(nbtName)
            itemStack.novaCompoundOrNull?.remove("nova", cbfName)
        }
        
        private fun NamespacedCompound.getEnchantmentsOrNull(name: String): MutableMap<Enchantment, Int>? =
            get("nova", name)
        
        private fun NamespacedCompound.getEnchantments(name: String): MutableMap<Enchantment, Int> =
            getOrPut("nova", name, ::HashMap)
        
        private fun CompoundTag.getEnchantmentsOrNull(name: String): ListTag? =
            getOrNull(name)
        
        private fun CompoundTag.getEnchantments(name: String): ListTag =
            getOrPut(name, ::ListTag)
        
    }
    
}