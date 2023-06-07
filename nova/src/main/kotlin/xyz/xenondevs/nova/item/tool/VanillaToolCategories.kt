package xyz.xenondevs.nova.item.tool

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.Tag
import xyz.xenondevs.commons.collections.associateWithNotNullTo
import xyz.xenondevs.commons.collections.enumMapOf
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.registry.NovaRegistries.TOOL_CATEGORY
import xyz.xenondevs.nova.util.set
import java.util.function.Predicate

@InternalInit(stage = InternalInitStage.PRE_WORLD)
object VanillaToolCategories {
    
    val SHOVEL = register(
        "shovel",
        canDoSweepAttack = false, canBreakBlocksInCreative = true,
        itemDamageOnBreakBlock = 1, itemDamageOnAttackEntity = 2,
        genericMultipliers = mapOf(
            Material.WOODEN_SHOVEL to 2.0,
            Material.STONE_SHOVEL to 4.0,
            Material.IRON_SHOVEL to 6.0,
            Material.DIAMOND_SHOVEL to 8.0,
            Material.NETHERITE_SHOVEL to 9.0,
            Material.GOLDEN_SHOVEL to 12.0
        )
    )
    
    val PICKAXE = register(
        "pickaxe",
        canDoSweepAttack = false, canBreakBlocksInCreative = true,
        itemDamageOnBreakBlock = 1, itemDamageOnAttackEntity = 2,
        genericMultipliers = mapOf(
            Material.WOODEN_PICKAXE to 2.0,
            Material.STONE_PICKAXE to 4.0,
            Material.IRON_PICKAXE to 6.0,
            Material.DIAMOND_PICKAXE to 8.0,
            Material.NETHERITE_PICKAXE to 9.0,
            Material.GOLDEN_PICKAXE to 12.0
        )
    )
    
    val AXE = register(
        "axe",
        canDoSweepAttack = false, canBreakBlocksInCreative = true,
        itemDamageOnBreakBlock = 1, itemDamageOnAttackEntity = 2,
        genericMultipliers = mapOf(
            Material.WOODEN_AXE to 2.0,
            Material.STONE_AXE to 4.0,
            Material.IRON_AXE to 6.0,
            Material.DIAMOND_AXE to 8.0,
            Material.NETHERITE_AXE to 9.0,
            Material.GOLDEN_AXE to 12.0
        )
    )
    
    val HOE = register(
        "hoe",
        canDoSweepAttack = false, canBreakBlocksInCreative = true,
        itemDamageOnBreakBlock = 1, itemDamageOnAttackEntity = 2,
        genericMultipliers = mapOf(
            Material.WOODEN_HOE to 2.0,
            Material.STONE_HOE to 4.0,
            Material.IRON_HOE to 6.0,
            Material.DIAMOND_HOE to 8.0,
            Material.NETHERITE_HOE to 9.0,
            Material.GOLDEN_HOE to 12.0
        )
    )
    
    val SWORD = register(
        "sword",
        canDoSweepAttack = true, canBreakBlocksInCreative = false,
        itemDamageOnBreakBlock = 2, itemDamageOnAttackEntity = 1,
        genericMultipliers = mapOf(
            Material.WOODEN_SWORD to 1.5,
            Material.STONE_SWORD to 1.5,
            Material.IRON_SWORD to 1.5,
            Material.DIAMOND_SWORD to 1.5,
            Material.NETHERITE_SWORD to 1.5,
            Material.GOLDEN_SWORD to 1.5
        ),
        specialMultipliers = mapOf(
            // Cobwebs can be broken faster with swords with a speed multiplier of 15.
            // BambooSaplingBlock and BambooStalkBlock both always return 1.0 als the destroy progress if the tool is a sword.
            // Since that is not possible here, we just set the multiplier to a very high value.
            Material.WOODEN_SWORD to mapOf(
                Predicate<Material> { it == Material.COBWEB } to 15.0,
                Predicate<Material> { it == Material.BAMBOO || it == Material.BAMBOO_SAPLING } to 999999999.0
            ),
            Material.STONE_SWORD to mapOf(
                Predicate<Material> { it == Material.COBWEB } to 15.0,
                Predicate<Material> { it == Material.BAMBOO || it == Material.BAMBOO_SAPLING } to 999999999.0
            ),
            Material.IRON_SWORD to mapOf(
                Predicate<Material> { it == Material.COBWEB } to 15.0,
                Predicate<Material> { it == Material.BAMBOO || it == Material.BAMBOO_SAPLING } to 999999999.0
            ),
            Material.DIAMOND_SWORD to mapOf(
                Predicate<Material> { it == Material.COBWEB } to 15.0,
                Predicate<Material> { it == Material.BAMBOO || it == Material.BAMBOO_SAPLING } to 999999999.0
            ),
            Material.NETHERITE_SWORD to mapOf(
                Predicate<Material> { it == Material.COBWEB } to 15.0,
                Predicate<Material> { it == Material.BAMBOO || it == Material.BAMBOO_SAPLING } to 999999999.0
            ),
            Material.GOLDEN_SWORD to mapOf(
                Predicate<Material> { it == Material.COBWEB } to 15.0,
                Predicate<Material> { it == Material.BAMBOO || it == Material.BAMBOO_SAPLING } to 999999999.0
            ),
        )
    )
    
    val SHEARS = register(
        "shears",
        canDoSweepAttack = false, canBreakBlocksInCreative = true,
        itemDamageOnBreakBlock = 1, itemDamageOnAttackEntity = 0,
        genericMultipliers = mapOf(
            Material.SHEARS to 1.5
        ),
        specialMultipliers = mapOf(
            Material.SHEARS to mapOf(
                Predicate<Material> { it == Material.VINE || it == Material.GLOW_LICHEN } to 1.0,
                Predicate<Material> { Tag.WOOL.isTagged(it) } to 5.0,
                Predicate<Material> { Tag.LEAVES.isTagged(it) || it == Material.COBWEB } to 15.0
            )
        )
    )
    
    @JvmName("register1")
    private fun register(
        name: String,
        canDoSweepAttack: Boolean, canBreakBlocksInCreative: Boolean,
        itemDamageOnAttackEntity: Int, itemDamageOnBreakBlock: Int,
        genericMultipliers: Map<Material, Double>
    ): VanillaToolCategory {
        return register(
            name,
            canDoSweepAttack, canBreakBlocksInCreative,
            itemDamageOnAttackEntity, itemDamageOnBreakBlock,
            genericMultipliers,
            emptyMap<Material, Map<Material, Double>>()
        )
    }
    
    @Suppress("DEPRECATION")
    @JvmName("register2")
    private fun register(
        name: String,
        canDoSweepAttack: Boolean, canBreakBlocksInCreative: Boolean,
        itemDamageOnAttackEntity: Int, itemDamageOnBreakBlock: Int,
        genericMultipliers: Map<Material, Double>,
        specialMultipliers: Map<Material, Map<Predicate<Material>, Double>>
    ): VanillaToolCategory {
        val flatSpecialMultipliers = specialMultipliers.mapValuesTo(enumMapOf()) { (_, map) ->
            Material.values()
                .filter { it.isBlock && !it.isLegacy }
                .associateWithNotNullTo(enumMapOf()) { material ->
                    map.entries.firstOrNull { it.key.test(material) }?.value
                }
        }
        
        return register(
            name,
            canDoSweepAttack, canBreakBlocksInCreative,
            itemDamageOnAttackEntity, itemDamageOnBreakBlock,
            genericMultipliers,
            flatSpecialMultipliers
        )
    }
    
    private fun register(
        name: String,
        canDoSweepAttack: Boolean, canBreakBlocksInCreative: Boolean,
        itemDamageOnAttackEntity: Int, itemDamageOnBreakBlock: Int,
        genericMultipliers: Map<Material, Double>,
        specialMultipliers: Map<Material, Map<Material, Double>>
    ): VanillaToolCategory {
        val id = ResourceLocation("minecraft", name)
        val category = VanillaToolCategory(
            id,
            canDoSweepAttack, canBreakBlocksInCreative,
            itemDamageOnAttackEntity, itemDamageOnBreakBlock,
            genericMultipliers,
            specialMultipliers
        )
        TOOL_CATEGORY[id] = category
        return category
    }
    
}