package xyz.xenondevs.nova.compiler

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class RegistryEntryComparisonTest : CompilerPluginTest(
    "NOVA_REGISTRY_ENTRY_COMPARISON",
    "Keyed.kt" to "package org.bukkit; interface Keyed",
    "RegistryEntry.kt" to """
        package xyz.xenondevs.nova.registry

        interface NovaRegistryElement<out S : NovaRegistryElement<S>> : org.bukkit.Keyed
        sealed interface RegistryEntry<out T : org.bukkit.Keyed> {
            sealed interface Paper<out T : org.bukkit.Keyed> : RegistryEntry<T>
            sealed interface Nova<out T : NovaRegistryElement<T>> : RegistryEntry<T>
        }
        abstract class NovaItem : NovaRegistryElement<NovaItem>
        abstract class ItemType : org.bukkit.Keyed
    """
) {
    
    @ParameterizedTest
    @MethodSource("badConcreteComparisons")
    fun `reports comparisons with concrete values`(left: String, operation: String, right: String) = assertDiagnostics(
        """
            import xyz.xenondevs.nova.registry.*

            fun test(left: $left, right: $right) {
                left $operation right // warn
            }
        """
    )
    
    @ParameterizedTest
    @MethodSource("badConcreteWhenComparisons")
    fun `reports when comparisons with concrete values`(subject: String, branch: String) = assertDiagnostics(
        """
            import xyz.xenondevs.nova.registry.*

            fun test(subject: $subject, branch: $branch) {
                when (subject) {
                    branch -> Unit // warn
                    else -> Unit
                }
            }
        """
    )
    
    @ParameterizedTest
    @MethodSource("badGenericComparisons")
    fun `reports comparisons with generic values`(bound: String, left: String, operation: String, right: String) = assertDiagnostics(
        """
            import org.bukkit.Keyed
            import xyz.xenondevs.nova.registry.*

            fun <T : $bound> test(left: $left, right: $right) {
                left $operation right // warn
            }
        """
    )
    
    @ParameterizedTest
    @MethodSource("badGenericWhenComparisons")
    fun `reports when comparisons with generic values`(bound: String, subject: String, branch: String) = assertDiagnostics(
        """
            import org.bukkit.Keyed
            import xyz.xenondevs.nova.registry.*

            fun <T : $bound> test(subject: $subject, branch: $branch) {
                when (subject) {
                    branch -> Unit // warn
                    else -> Unit
                }
            }
        """
    )
    
    @ParameterizedTest
    @MethodSource("goodConcreteComparisons")
    fun `allows comparisons between concrete entries`(left: String, operation: String, right: String) = assertDiagnostics(
        """
            import xyz.xenondevs.nova.registry.*

            fun test(left: $left, right: $right) {
                left $operation right
            }
        """
    )
    
    @ParameterizedTest
    @MethodSource("goodConcreteWhenComparisons")
    fun `allows when comparisons between concrete entries`(subject: String, branch: String) = assertDiagnostics(
        """
            import xyz.xenondevs.nova.registry.*

            fun test(subject: $subject, branch: $branch) {
                when (subject) {
                    branch -> Unit
                    else -> Unit
                }
            }
        """
    )
    
    @ParameterizedTest
    @MethodSource("goodGenericComparisons")
    fun `allows comparisons between generic entries`(bound: String, left: String, operation: String, right: String) = assertDiagnostics(
        """
            import org.bukkit.Keyed
            import xyz.xenondevs.nova.registry.*

            fun <T : $bound> test(left: $left, right: $right) {
                left $operation right
            }
        """
    )
    
    @ParameterizedTest
    @MethodSource("goodGenericWhenComparisons")
    fun `allows when comparisons between generic entries`(bound: String, subject: String, branch: String) = assertDiagnostics(
        """
            import org.bukkit.Keyed
            import xyz.xenondevs.nova.registry.*

            fun <T : $bound> test(subject: $subject, branch: $branch) {
                when (subject) {
                    branch -> Unit
                    else -> Unit
                }
            }
        """
    )
    
    @Test
    fun `checks explicit equals calls`() = assertDiagnostics(
        """
            import xyz.xenondevs.nova.registry.*

            fun test(entry: RegistryEntry<ItemType>, value: ItemType, nullable: RegistryEntry<ItemType>?, other: RegistryEntry<ItemType>) {
                entry.equals(value) // warn
                value.equals(entry) // warn
                nullable?.equals(value) // warn
                entry.equals(other)
            }
        """
    )
    
    @Test
    fun `allows unrelated values null and identity comparisons`() = assertDiagnostics(
        """
            import xyz.xenondevs.nova.registry.*

            fun test(entry: RegistryEntry<ItemType>, value: ItemType, unrelated: NovaItem) {
                entry == unrelated
                entry == null
                entry === value
                entry !== value
            }
        """
    )
    
    @Test
    fun `supports suppression`() = assertDiagnostics(
        """
            @file:Suppress("NOVA_REGISTRY_ENTRY_COMPARISON")
            import xyz.xenondevs.nova.registry.*

            fun test(entry: RegistryEntry<ItemType>, value: ItemType) = entry == value
        """
    )
    
    companion object {
        
        private val novaEntries = listOf("RegistryEntry<NovaItem>", "RegistryEntry.Nova<NovaItem>").withNullableTypes()
        private val paperEntries = listOf("RegistryEntry<ItemType>", "RegistryEntry.Paper<ItemType>").withNullableTypes()
        private val novaValues = listOf("NovaItem").withNullableTypes()
        private val paperValues = listOf("ItemType").withNullableTypes()
        private val genericNovaEntries = listOf("RegistryEntry<T>", "RegistryEntry.Nova<T>").withNullableTypes()
        private val genericPaperEntries = listOf("RegistryEntry<T>", "RegistryEntry.Paper<T>").withNullableTypes()
        private val genericValues = listOf("T").withNullableTypes()
        
        @JvmStatic
        private fun badConcreteComparisons() =
            comparisons(novaEntries, novaValues, swapped = true) +
                comparisons(paperEntries, paperValues, swapped = true)
        
        @JvmStatic
        private fun badConcreteWhenComparisons() =
            whenComparisons(novaEntries, novaValues, swapped = true) +
                whenComparisons(paperEntries, paperValues, swapped = true)
        
        @JvmStatic
        private fun badGenericComparisons() =
            comparisons(genericNovaEntries, genericValues, swapped = true, "NovaRegistryElement<T>") +
                comparisons(genericPaperEntries, genericValues, swapped = true, "Keyed")
        
        @JvmStatic
        private fun badGenericWhenComparisons() =
            whenComparisons(genericNovaEntries, genericValues, swapped = true, "NovaRegistryElement<T>") +
                whenComparisons(genericPaperEntries, genericValues, swapped = true, "Keyed")
        
        @JvmStatic
        private fun goodConcreteComparisons() =
            comparisons(novaEntries, novaEntries) + comparisons(paperEntries, paperEntries)
        
        @JvmStatic
        private fun goodConcreteWhenComparisons() =
            whenComparisons(novaEntries, novaEntries) + whenComparisons(paperEntries, paperEntries)
        
        @JvmStatic
        private fun goodGenericComparisons() =
            comparisons(genericNovaEntries, genericNovaEntries, prefix = arrayOf("NovaRegistryElement<T>")) +
                comparisons(genericPaperEntries, genericPaperEntries, prefix = arrayOf("Keyed"))
        
        @JvmStatic
        private fun goodGenericWhenComparisons() =
            whenComparisons(genericNovaEntries, genericNovaEntries, prefix = arrayOf("NovaRegistryElement<T>")) +
                whenComparisons(genericPaperEntries, genericPaperEntries, prefix = arrayOf("Keyed"))
        
        private fun comparisons(
            leftTypes: List<String>,
            rightTypes: List<String>,
            swapped: Boolean = false,
            vararg prefix: String
        ): List<Arguments> = buildList {
            for (left in leftTypes) {
                for (right in rightTypes) {
                    for (operation in listOf("==", "!=")) {
                        add(Arguments.of(*prefix, left, operation, right))
                        if (swapped)
                            add(Arguments.of(*prefix, right, operation, left))
                    }
                }
            }
        }
        
        private fun whenComparisons(
            subjectTypes: List<String>,
            branchTypes: List<String>,
            swapped: Boolean = false,
            vararg prefix: String
        ): List<Arguments> = buildList {
            for (subject in subjectTypes) {
                for (branch in branchTypes) {
                    add(Arguments.of(*prefix, subject, branch))
                    if (swapped)
                        add(Arguments.of(*prefix, branch, subject))
                }
            }
        }
        
        private fun List<String>.withNullableTypes(): List<String> = flatMap { listOf(it, "$it?") }
    }
}
