package xyz.xenondevs.nova.detekt

import dev.detekt.api.Config
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import org.junit.jupiter.api.Test

class MaterialUsageRuleTest {

    private val env = createEnvironment()
    private val typeStubs = arrayOf(
        """
        package org.bukkit
        enum class Material {
            STONE;

            val something: String
                get() = name
        }

        class Block(val type: Material)
        """,
        """
        package org.bukkit.inventory
        interface ItemType
        """,
        """
        package org.bukkit.block
        interface BlockType
        """
    )

    @Test
    fun `reports material type references and expressions`() {
        val code = """
            import org.bukkit.Material

            val field: Material = Material.STONE
            fun accepts(material: Material) = Unit
            fun returns(): Material = Material.STONE
            fun inferred() = Material.STONE
            fun nested(): List<Material> = listOf(Material.STONE)
            fun call() = accepts(Material.STONE)
        """

        val findings = MaterialUsageRule(Config.empty).lintWithContext(env, code, *typeStubs)
        assert(findings.isNotEmpty()) { findings }
    }

    @Test
    fun `allows item type and block type`() {
        val code = """
            import org.bukkit.block.BlockType
            import org.bukkit.inventory.ItemType

            fun item(type: ItemType): ItemType = type
            fun block(type: BlockType): BlockType = type
        """

        val findings = MaterialUsageRule(Config.empty).lintWithContext(env, code, *typeStubs)
        assert(findings.isEmpty()) { findings }
    }

    @Test
    fun `reports implicit material receiver usage`() {
        val code = """
            fun read(block: org.bukkit.Block): String = block.type.something
        """

        val findings = MaterialUsageRule(Config.empty).lintWithContext(env, code, *typeStubs)
        assert(findings.isNotEmpty()) { findings }
    }
}
