package xyz.xenondevs.nova.compiler

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.preprocessors.BackendDependentSourceFilePreprocessor
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.utils.ReplacingSourceTransformer
import org.jetbrains.kotlin.utils.bind
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.Parameter
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@ParameterizedClass
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractRegistryEntryComparisonJvmDiagnosticTest : AbstractNovaJvmDiagnosticTest() {
    open val bound: String? get() = null

    init {
        configure {
            bound?.let { useSourcePreprocessor(::ParameterPreprocessor.bind(BOUND, it)) }
        }
    }

    companion object {
        const val BOUND = "BOUND"
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
            comparisons(genericNovaEntries, genericNovaEntries, suffix = arrayOf("NovaRegistryElement<T>")) +
                    comparisons(genericPaperEntries, genericPaperEntries, suffix = arrayOf("Keyed"))

        @JvmStatic
        private fun goodGenericWhenComparisons() =
            whenComparisons(genericNovaEntries, genericNovaEntries, suffix = arrayOf("NovaRegistryElement<T>")) +
                    whenComparisons(genericPaperEntries, genericPaperEntries, suffix = arrayOf("Keyed"))

        private fun comparisons(
            leftTypes: List<String>,
            rightTypes: List<String>,
            swapped: Boolean = false,
            vararg suffix: String
        ): List<Arguments> = buildList {
            for (left in leftTypes) {
                for (right in rightTypes) {
                    for (operation in listOf("==", "!=")) {
                        add(Arguments.of(left, operation, right, *suffix))
                        if (swapped)
                            add(Arguments.of(right, operation, left, *suffix))
                    }
                }
            }
        }

        private fun whenComparisons(
            subjectTypes: List<String>,
            branchTypes: List<String>,
            swapped: Boolean = false,
            vararg suffix: String
        ): List<Arguments> = buildList {
            for (subject in subjectTypes) {
                for (branch in branchTypes) {
                    add(Arguments.of(subject, branch, *suffix))
                    if (swapped)
                        add(Arguments.of(branch, subject, *suffix))
                }
            }
        }

        private fun List<String>.withNullableTypes(): List<String> = flatMap { listOf(it, "$it?") }
    }

    class ParameterPreprocessor(testServices: TestServices, val from: String, val to: String) :
        BackendDependentSourceFilePreprocessor(testServices) {
        override fun selectTransformer(targetBackend: TargetBackend): ReplacingSourceTransformer =
            ReplacingSourceTransformer(from, to)
    }
}

abstract class AbstractComparisonsJvmDiagnosticTest : AbstractRegistryEntryComparisonJvmDiagnosticTest() {
    @Parameter(0)
    lateinit var left: String

    @Parameter(1)
    lateinit var operation: String

    @Parameter(2)
    lateinit var right: String

    init {
        configure {
            useSourcePreprocessor(::ParameterPreprocessor.bind(LEFT, left))
            useSourcePreprocessor(::ParameterPreprocessor.bind(OPERATION, operation))
            useSourcePreprocessor(::ParameterPreprocessor.bind(RIGHT, right))
            bound?.let { useSourcePreprocessor(::ParameterPreprocessor.bind(BOUND, it)) }
        }
    }

    companion object {
        const val LEFT = "LEFT"
        const val OPERATION = "OPERATION"
        const val RIGHT = "RIGHT"
    }
}

abstract class AbstractWhenComparisonsJvmDiagnosticTest : AbstractRegistryEntryComparisonJvmDiagnosticTest() {
    @Parameter(0)
    lateinit var subject: String

    @Parameter(1)
    lateinit var branch: String

    init {
        configure {
            useSourcePreprocessor(::ParameterPreprocessor.bind(SUBJECT, subject))
            useSourcePreprocessor(::ParameterPreprocessor.bind(BRANCH, branch))
            bound?.let { useSourcePreprocessor(::ParameterPreprocessor.bind(BOUND, it)) }
        }
    }

    companion object {
        const val SUBJECT = "SUBJECT"
        const val BRANCH = "BRANCH"
    }
}

@MethodSource("badConcreteComparisons")
abstract class AbstractBadConcreteComparisonsJvmDiagnosticTest : AbstractComparisonsJvmDiagnosticTest()

@MethodSource("badConcreteWhenComparisons")
abstract class AbstractBadConcreteWhenComparisonsJvmDiagnosticTest : AbstractWhenComparisonsJvmDiagnosticTest()

@MethodSource("badGenericComparisons")
abstract class AbstractBadGenericComparisonsJvmDiagnosticTest : AbstractComparisonsJvmDiagnosticTest() {
    @Parameter(3)
    override lateinit var bound: String
}

@MethodSource("badGenericWhenComparisons")
abstract class AbstractBadGenericWhenComparisonsJvmDiagnosticTest : AbstractWhenComparisonsJvmDiagnosticTest() {
    @Parameter(2)
    override lateinit var bound: String
}

@MethodSource("goodConcreteComparisons")
abstract class AbstractGoodConcreteComparisonsJvmDiagnosticTest : AbstractComparisonsJvmDiagnosticTest()

@MethodSource("goodConcreteWhenComparisons")
abstract class AbstractGoodConcreteWhenComparisonsJvmDiagnosticTest : AbstractWhenComparisonsJvmDiagnosticTest()

@MethodSource("goodGenericComparisons")
abstract class AbstractGoodGenericComparisonsJvmDiagnosticTest : AbstractComparisonsJvmDiagnosticTest() {
    @Parameter(3)
    override lateinit var bound: String
}

@MethodSource("goodGenericWhenComparisons")
abstract class AbstractGoodGenericWhenComparisonsJvmDiagnosticTest : AbstractWhenComparisonsJvmDiagnosticTest() {
    @Parameter(2)
    override lateinit var bound: String
}
