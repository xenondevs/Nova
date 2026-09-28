package xyz.xenondevs.nova.compiler

import org.junit.jupiter.api.Test

internal class MaterialUsageTest : CompilerPluginTest(
    "NOVA_MATERIAL_USAGE",
    "Material.kt" to """
        @file:Suppress("NOVA_MATERIAL_USAGE")
        package org.bukkit
        enum class Material {
            STONE;
            val something: String get() = name
        }
        class Block(val type: Material)
        fun getMaterial(): Material = Material.STONE
    """,
    "ItemType.kt" to "package org.bukkit.inventory; interface ItemType",
    "BlockType.kt" to "package org.bukkit.block; interface BlockType"
) {
    
    @Test
    fun `reports explicit type references`() = assertDiagnostics(
        """
            import org.bukkit.Material

            lateinit var field: Material // warn
            fun accepts(material: Material) = Unit // warn
            fun returns(): Material = error("missing") // warn
        """
    )
    
    @Test
    fun `reports nested types and inferred expressions`() = assertDiagnostics(
        """
            import org.bukkit.Material

            lateinit var materials: List<Material> // warn 2
            lateinit var nested: List<Map<String, Material?>> // warn 3
            val field = Material.STONE // warn
            fun inferred() = Material.STONE // warn
            val strings: List<String> = emptyList()
        """
    )
    
    @Test
    fun `reports material hidden by type aliases`() = assertDiagnostics(
        """
            import org.bukkit.Material

            typealias Materials = List<Material> // warn 2
            lateinit var materials: Materials // warn
        """
    )
    
    @Test
    fun `reports function results including inferred declarations and receivers`() = assertDiagnostics(
        """
            import org.bukkit.Material
            import org.bukkit.getMaterial

            fun test() {
                Material.valueOf("STONE") // warn
                getMaterial().name // warn
                val inferred = getMaterial() // warn
                val nested = listOf(getMaterial()) // warn 2
            }
        """
    )
    
    @Test
    fun `reports material arguments and property receivers`() = assertDiagnostics(
        """
            import org.bukkit.Material
            fun accept(value: Any) = Unit
            fun test(block: org.bukkit.Block) {
                accept(Material.STONE) // warn
                block.type.something // warn
            }
        """
    )
    
    @Test
    fun `allows item types block types and unrelated generics`() = assertDiagnostics(
        """
            import org.bukkit.block.BlockType
            import org.bukkit.inventory.ItemType

            fun item(type: ItemType): ItemType = type
            fun block(type: BlockType): BlockType = type
            val strings: List<String> = emptyList()
            val nested: List<Map<String, ItemType>> = emptyList()
        """
    )
    
    @Test
    fun `reports material class literals`() = assertDiagnostics(
        """
            import org.bukkit.Material
            import org.bukkit.inventory.ItemType

            val materialClass = Material::class // warn
            fun materialClass() = Material::class // warn
            val itemClass = ItemType::class

            @Suppress("NOVA_MATERIAL_USAGE")
            val suppressed = Material::class
        """
    )
    
    @Test
    fun `supports suppression`() = assertDiagnostics(
        """
            @file:Suppress("NOVA_MATERIAL_USAGE")
            import org.bukkit.Material
            val material: Material = Material.valueOf("STONE")
        """
    )
}
