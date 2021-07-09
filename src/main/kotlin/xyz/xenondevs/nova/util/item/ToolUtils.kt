package xyz.xenondevs.nova.util.item

import org.bukkit.Material

fun Material.isShovel() = name.endsWith("_SHOVEL")

fun Material.isPickaxe() = name.endsWith("_PICKAXE")

fun Material.isAxe() = name.endsWith("_AXE")

fun Material.isHoe() = name.endsWith("_HOE")

fun Material.isSword() = name.endsWith("_SWORD")

