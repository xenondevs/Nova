package xyz.xenondevs.nova.ui.menu.item.recipes.group.hardcoded

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.recipe.NovaRecipe
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.impl.processing.CobblestoneGenerator
import xyz.xenondevs.nova.tileentity.impl.processing.Freezer

object HardcodedRecipes {
    
    val recipes: List<NovaRecipe> = listOf(
        StarCollectorRecipe,
        CobblestoneGeneratorRecipe(NamespacedKey(NOVA, "cobblestone_generator.cobblestone"), CobblestoneGenerator.Mode.COBBLESTONE),
        CobblestoneGeneratorRecipe(NamespacedKey(NOVA, "cobblestone_generator.stone"), CobblestoneGenerator.Mode.STONE),
        CobblestoneGeneratorRecipe(NamespacedKey(NOVA, "cobblestone_generator.obsidian"), CobblestoneGenerator.Mode.OBSIDIAN),
        FreezerRecipe(NamespacedKey(NOVA, "freezer.ice"), Freezer.Mode.ICE),
        FreezerRecipe(NamespacedKey(NOVA, "freezer.packed_ice"), Freezer.Mode.PACKED_ICE),
        FreezerRecipe(NamespacedKey(NOVA, "freezer.blue_ice"), Freezer.Mode.BLUE_ICE),
    )
    
}

 object StarCollectorRecipe : NovaRecipe {
    override val key = NamespacedKey(NOVA, "star_collector.star_dust")
    override val result = NovaMaterialRegistry.STAR_DUST.createItemStack()
}

 class CobblestoneGeneratorRecipe(
    override val key: NamespacedKey,
    val mode: CobblestoneGenerator.Mode,
    override val result: ItemStack = mode.product
) : NovaRecipe

 class FreezerRecipe(
    override val key: NamespacedKey,
    val mode: Freezer.Mode,
    override val result: ItemStack = mode.product
) : NovaRecipe