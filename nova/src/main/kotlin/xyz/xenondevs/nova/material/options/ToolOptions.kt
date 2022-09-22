package xyz.xenondevs.nova.material.options

import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.ToolLevel

data class ToolOptions(
    val level: ToolLevel,
    val category: ToolCategory,
    val speedMultiplier: Double
)

data class DamageOptions(
    val attackDamage: Double,
    val attackSpeed: Double
)