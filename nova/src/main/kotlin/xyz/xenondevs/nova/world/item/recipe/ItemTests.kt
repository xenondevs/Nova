package xyz.xenondevs.nova.world.item.recipe

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.nova.util.item.customModelData
import xyz.xenondevs.nova.util.item.namelessCopyOrSelf
import xyz.xenondevs.nova.util.item.novaItem

interface ItemTest {
    fun test(item: ItemStack): Boolean
}

interface SingleItemTest : ItemTest {
    val example: ItemStack
}

interface MultiItemTest : ItemTest {
    val examples: List<ItemStack>
}

class ModelDataTest(private val type: Material, private val data: IntArray, override val example: ItemStack) : SingleItemTest {
    
    override fun test(item: ItemStack): Boolean {
        return item.type == type && item.customModelData in data
    }
    
}

class TagTest(private val tag: Tag<Material>, override val examples: List<ItemStack> = tag.values.map(::ItemStack)) : MultiItemTest {
    
    override fun test(item: ItemStack): Boolean {
        return tag.isTagged(item.type) && item.customModelData == 0
    }
    
}

class NovaIdTest(private val id: String, override val example: ItemStack) : SingleItemTest {
    
    override fun test(item: ItemStack): Boolean {
        return item.novaItem?.id.toString() == id
    }
    
}

class NovaNameTest(private val name: String, override val examples: List<ItemStack>) : MultiItemTest {
    
    override fun test(item: ItemStack): Boolean {
        return item.novaItem?.id?.value() == name
    }
    
}

class ComplexTest(override val example: ItemStack) : SingleItemTest {
    
    override fun test(item: ItemStack): Boolean {
        val testStack = item.namelessCopyOrSelf
        return example.isSimilar(testStack)
    }
    
}

class CustomRecipeChoice(private val tests: List<ItemTest>) : RecipeChoice.ExactChoice(
    tests.flatMap {
        when (it) {
            is SingleItemTest -> listOf(it.example)
            is MultiItemTest -> it.examples
            else -> throw UnsupportedOperationException()
        }
    }
) {
    
    override fun test(item: ItemStack): Boolean {
        return tests.any { it.test(item) }
    }
    
}