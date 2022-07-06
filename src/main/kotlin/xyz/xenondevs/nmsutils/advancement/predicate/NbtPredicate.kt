package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.TagParser
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nmsutils.adapter.NonNullAdapter
import xyz.xenondevs.nmsutils.internal.util.nmsStack
import net.minecraft.advancements.critereon.NbtPredicate as MojangNbtPredicate

class NbtPredicate(
    val nbt: Any
) : Predicate {
    
    companion object : NonNullAdapter<NbtPredicate, MojangNbtPredicate>(MojangNbtPredicate.ANY) {
        
        override fun convert(value: NbtPredicate): MojangNbtPredicate {
            return MojangNbtPredicate(
                when (val nbt = value.nbt) {
                    is String -> TagParser.parseTag(nbt)
                    is ItemStack -> nbt.nmsStack.tag
                    is CompoundTag -> nbt
                    else -> throw UnsupportedOperationException("Invalid nbt type: ${nbt::class.java}")
                }
            )
        }
        
    }
    
}