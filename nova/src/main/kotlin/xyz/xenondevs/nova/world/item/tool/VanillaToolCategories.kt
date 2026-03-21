package xyz.xenondevs.nova.world.item.tool

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.RegistryLoader
import xyz.xenondevs.nova.registry.RegistryEntry

@InternalInit(
    stage = InternalInitStage.PRE_WORLD,
    runBefore = [RegistryLoader::class]
)
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
    ): RegistryEntry.Nova<ToolCategory> {
        val id = Key.key(name)
        return RegistryLoader.enqueueNova(NovaRegistries.INTERNAL_TOOL_CATEGORY, id) {
            VanillaToolCategory(
                it,
                canDoSweepAttack, canBreakBlocksInCreative,
                itemDamageOnAttackEntity, itemDamageOnBreakBlock
            )
        }
    }
    
}