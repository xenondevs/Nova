package xyz.xenondevs.nova.world.item.tool

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.NovaRegistries.TOOL_CATEGORY
import xyz.xenondevs.nova.util.set

@InternalInit(stage = InternalInitStage.PRE_WORLD)
object VanillaToolCategories {
    
    val SHOVEL = register(
        "shovel",
        canDoSweepAttack = false, canBreakBlocksInCreative = true,
        itemDamageOnBreakBlock = 1, itemDamageOnAttackEntity = 2
    )
    
    val PICKAXE = register(
        "pickaxe",
        canDoSweepAttack = false, canBreakBlocksInCreative = true,
        itemDamageOnBreakBlock = 1, itemDamageOnAttackEntity = 2
    )
    
    val AXE = register(
        "axe",
        canDoSweepAttack = false, canBreakBlocksInCreative = true,
        itemDamageOnBreakBlock = 1, itemDamageOnAttackEntity = 2
    )
    
    val HOE = register(
        "hoe",
        canDoSweepAttack = false, canBreakBlocksInCreative = true,
        itemDamageOnBreakBlock = 1, itemDamageOnAttackEntity = 2
    )
    
    val SWORD = register(
        "sword",
        canDoSweepAttack = true, canBreakBlocksInCreative = false,
        itemDamageOnBreakBlock = 2, itemDamageOnAttackEntity = 1
    )
    
    val SHEARS = register(
        "shears",
        canDoSweepAttack = false, canBreakBlocksInCreative = true,
        itemDamageOnBreakBlock = 1, itemDamageOnAttackEntity = 0
    )
    
    private fun register(
        name: String,
        canDoSweepAttack: Boolean, canBreakBlocksInCreative: Boolean,
        itemDamageOnAttackEntity: Int, itemDamageOnBreakBlock: Int
    ): VanillaToolCategory {
        val id = ResourceLocation.withDefaultNamespace(name)
        val category = VanillaToolCategory(
            id,
            canDoSweepAttack, canBreakBlocksInCreative,
            itemDamageOnAttackEntity, itemDamageOnBreakBlock
        )
        TOOL_CATEGORY[id] = category
        return category
    }
    
}