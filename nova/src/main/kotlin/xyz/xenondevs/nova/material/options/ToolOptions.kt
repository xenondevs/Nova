package xyz.xenondevs.nova.material.options

import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.ToolLevel

data class ToolOptions(
    val level: ToolLevel,
    val category: ToolCategory,
    val breakSpeedMultiplier: Double,
    val attackDamage: Double,
    val attackSpeed: Double
)