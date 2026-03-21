package xyz.xenondevs.nova.detekt

import dev.detekt.api.Config
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class RegistryEntryComparisonRuleTest {
    
    private val env = createEnvironment()
    private val typeStubs = arrayOf(
        """
        package org.bukkit
        interface Keyed
        """,
        """
        package xyz.xenondevs.nova.registry
        interface NovaRegistryElement<out S : NovaRegistryElement<S>> : org.bukkit.Keyed
        sealed interface RegistryEntry<out T : org.bukkit.Keyed> {
            sealed interface Paper<out T : org.bukkit.Keyed> : RegistryEntry<T>
            sealed interface Nova<out T : NovaRegistryElement<T>> : RegistryEntry<T>
        }
        """,
        """
        package xyz.xenondevs.nova.detekt
        import org.bukkit.Keyed
        import xyz.xenondevs.nova.registry.NovaRegistryElement
        abstract class NovaItem : NovaRegistryElement<NovaItem>
        abstract class ItemType : Keyed
        """
    )
    
    @MethodSource("badConcreteComparisons")
    @ParameterizedTest
    fun `reports bad comparison with concrete type`(left: String, cmp: String, right: String) {
        val code = """
            import xyz.xenondevs.nova.registry.RegistryEntry
            import xyz.xenondevs.nova.detekt.NovaItem
            import xyz.xenondevs.nova.detekt.ItemType
    
            fun test(entry: $left, item: $right) {
                if (entry $cmp item) println()
            }
        """
        val findings = RegistryEntryComparisonRule(Config.empty).lintWithContext(env, code, *typeStubs)
        assert(findings.size == 1) { findings }
    }
    
    @MethodSource("badConcreteWhenComparisons")
    @ParameterizedTest
    fun `reports bad when comparison with concrete type`(subject: String, branch: String) {
        val code = """
            import xyz.xenondevs.nova.registry.RegistryEntry
            import xyz.xenondevs.nova.detekt.NovaItem
            import xyz.xenondevs.nova.detekt.ItemType
    
            fun test(subject: $subject, branch: $branch) {
                when (subject) {
                    branch -> Unit
                    else -> Unit
                }
            }
        """
        val findings = RegistryEntryComparisonRule(Config.empty).lintWithContext(env, code, *typeStubs)
        assert(findings.size == 1) { findings }
    }
    
    @MethodSource("badGenericComparisons")
    @ParameterizedTest
    fun `reports bad comparison with generic type`(bound: String, left: String, cmp: String, right: String) {
        val code = """
            import xyz.xenondevs.nova.registry.RegistryEntry
            import xyz.xenondevs.nova.registry.NovaRegistryElement
            import org.bukkit.Keyed

            fun <T : $bound> test(entry: $left, item: $right) {
                if (entry $cmp item) println()
            }
        """
        val findings = RegistryEntryComparisonRule(Config.empty).lintWithContext(env, code, *typeStubs)
        assert(findings.size == 1) { findings }
    }
    
    @MethodSource("badGenericWhenComparisons")
    @ParameterizedTest
    fun `reports bad when comparison with generic type`(bound: String, subject: String, branch: String) {
        val code = """
            import xyz.xenondevs.nova.registry.RegistryEntry
            import xyz.xenondevs.nova.registry.NovaRegistryElement
            import org.bukkit.Keyed

            fun <T : $bound> test(subject: $subject, branch: $branch) {
                when (subject) {
                    branch -> Unit
                    else -> Unit
                }
            }
        """
        val findings = RegistryEntryComparisonRule(Config.empty).lintWithContext(env, code, *typeStubs)
        assert(findings.size == 1) { findings }
    }
    
    @MethodSource("goodConcreteComparisons")
    @ParameterizedTest
    fun `does not report good comparison with concrete type`(left: String, cmp: String, right: String) {
        val code = """
            import xyz.xenondevs.nova.registry.RegistryEntry
            import xyz.xenondevs.nova.detekt.NovaItem
            import xyz.xenondevs.nova.detekt.ItemType

            fun test(entry: $left, item: $right) {
                if (entry $cmp item) println()
            }
        """
        val findings = RegistryEntryComparisonRule(Config.empty).lintWithContext(env, code, *typeStubs)
        assert(findings.isEmpty()) { findings }
    }
    
    @MethodSource("goodConcreteWhenComparisons")
    @ParameterizedTest
    fun `does not report good when comparison with concrete type`(subject: String, branch: String) {
        val code = """
            import xyz.xenondevs.nova.registry.RegistryEntry
            import xyz.xenondevs.nova.detekt.NovaItem
            import xyz.xenondevs.nova.detekt.ItemType

            fun test(subject: $subject, branch: $branch) {
                when (subject) {
                    branch -> Unit
                    else -> Unit
                }
            }
        """
        val findings = RegistryEntryComparisonRule(Config.empty).lintWithContext(env, code, *typeStubs)
        assert(findings.isEmpty()) { findings }
    }
    
    @MethodSource("goodGenericComparisons")
    @ParameterizedTest
    fun `does not report good comparison with generic type`(bound: String, left: String, cmp: String, right: String) {
        val code = """
            import xyz.xenondevs.nova.registry.RegistryEntry
            import xyz.xenondevs.nova.registry.NovaRegistryElement
            import org.bukkit.Keyed

            fun <T : $bound> test(entry: $left, item: $right) {
                if (entry $cmp item) println()
            }
        """
        val findings = RegistryEntryComparisonRule(Config.empty).lintWithContext(env, code, *typeStubs)
        assert(findings.isEmpty()) { findings }
    }
    
    @MethodSource("goodGenericWhenComparisons")
    @ParameterizedTest
    fun `does not report good when comparison with generic type`(bound: String, subject: String, branch: String) {
        val code = """
            import xyz.xenondevs.nova.registry.RegistryEntry
            import xyz.xenondevs.nova.registry.NovaRegistryElement
            import org.bukkit.Keyed

            fun <T : $bound> test(subject: $subject, branch: $branch) {
                when (subject) {
                    branch -> Unit
                    else -> Unit
                }
            }
        """
        val findings = RegistryEntryComparisonRule(Config.empty).lintWithContext(env, code, *typeStubs)
        assert(findings.isEmpty()) { findings }
    }
    
    companion object {
        
        private val comparisons = listOf("==", "!=")
        private val novaEntryTypes = listOf("RegistryEntry<NovaItem>", "RegistryEntry.Nova<NovaItem>", "RegistryEntry<NovaItem>?", "RegistryEntry.Nova<NovaItem>?")
        private val paperEntryTypes = listOf("RegistryEntry<ItemType>", "RegistryEntry.Paper<ItemType>", "RegistryEntry<ItemType>?", "RegistryEntry.Paper<ItemType>?")
        private val novaConcreteTypes = listOf("NovaItem", "NovaItem?")
        private val paperConcreteTypes = listOf("ItemType", "ItemType?")
        private val novaGenericEntryTypes = listOf("RegistryEntry<T>", "RegistryEntry.Nova<T>", "RegistryEntry<T>?", "RegistryEntry.Nova<T>?")
        private val paperGenericEntryTypes = listOf("RegistryEntry<T>", "RegistryEntry.Paper<T>", "RegistryEntry<T>?", "RegistryEntry.Paper<T>?")
        private val genericTypeParams = listOf("T", "T?")
        
        private fun combos(
            leftTypes: List<String>, rightTypes: List<String>,
            swapped: Boolean = false,
            vararg prefix: String
        ): List<Arguments> = buildList {
            for (left in leftTypes) {
                for (right in rightTypes) {
                    for (cmp in comparisons) {
                        add(Arguments.of(*prefix, left, cmp, right))
                        if (swapped) add(Arguments.of(*prefix, right, cmp, left))
                    }
                }
            }
        }
        
        private fun whenCombos(
            subjectTypes: List<String>, branchTypes: List<String>,
            swapped: Boolean = false,
            vararg prefix: String
        ): List<Arguments> = buildList {
            for (subject in subjectTypes) {
                for (branch in branchTypes) {
                    add(Arguments.of(*prefix, subject, branch))
                    if (swapped) add(Arguments.of(*prefix, branch, subject))
                }
            }
        }
        
        @JvmStatic
        fun badConcreteComparisons() =
            combos(novaEntryTypes, novaConcreteTypes, swapped = true) +
                combos(paperEntryTypes, paperConcreteTypes, swapped = true)
        
        @JvmStatic
        fun badConcreteWhenComparisons() =
            whenCombos(novaEntryTypes, novaConcreteTypes, swapped = true) +
                whenCombos(paperEntryTypes, paperConcreteTypes, swapped = true)
        
        @JvmStatic
        fun badGenericComparisons() =
            combos(novaGenericEntryTypes, genericTypeParams, swapped = true, "NovaRegistryElement<T>") +
                combos(paperGenericEntryTypes, genericTypeParams, swapped = true, "Keyed")
        
        @JvmStatic
        fun badGenericWhenComparisons() =
            whenCombos(novaGenericEntryTypes, genericTypeParams, swapped = true, "NovaRegistryElement<T>") +
                whenCombos(paperGenericEntryTypes, genericTypeParams, swapped = true, "Keyed")
        
        @JvmStatic
        fun goodConcreteComparisons() =
            combos(novaEntryTypes, novaEntryTypes) +
                combos(paperEntryTypes, paperEntryTypes)
        
        @JvmStatic
        fun goodConcreteWhenComparisons() =
            whenCombos(novaEntryTypes, novaEntryTypes) +
                whenCombos(paperEntryTypes, paperEntryTypes)
        
        @JvmStatic
        fun goodGenericComparisons() =
            combos(novaGenericEntryTypes, novaGenericEntryTypes, prefix = arrayOf("NovaRegistryElement<T>")) +
                combos(paperGenericEntryTypes, paperGenericEntryTypes, prefix = arrayOf("Keyed"))
        
        @JvmStatic
        fun goodGenericWhenComparisons() =
            whenCombos(novaGenericEntryTypes, novaGenericEntryTypes, prefix = arrayOf("NovaRegistryElement<T>")) +
                whenCombos(paperGenericEntryTypes, paperGenericEntryTypes, prefix = arrayOf("Keyed"))
        
    }
    
}