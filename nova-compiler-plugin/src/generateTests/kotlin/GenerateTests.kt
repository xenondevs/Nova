package xyz.xenondevs.nova.compiler

import org.jetbrains.kotlin.compiler.plugin.devkit.generateDevKitTestsWithJUnit5

fun main() = generateDevKitTestsWithJUnit5 {
    val registryEntryComparisonParameterizedTests = mapOf(
        "badConcreteComparisons" to AbstractBadConcreteComparisonsJvmDiagnosticTest::class,
        "badConcreteWhenComparisons" to AbstractBadConcreteWhenComparisonsJvmDiagnosticTest::class,
        "badGenericComparisons" to AbstractBadGenericComparisonsJvmDiagnosticTest::class,
        "badGenericWhenComparisons" to AbstractBadGenericWhenComparisonsJvmDiagnosticTest::class,
        "goodConcreteComparisons" to AbstractGoodConcreteComparisonsJvmDiagnosticTest::class,
        "goodConcreteWhenComparisons" to AbstractGoodConcreteWhenComparisonsJvmDiagnosticTest::class,
        "goodGenericComparisons" to AbstractGoodGenericComparisonsJvmDiagnosticTest::class,
        "goodGenericWhenComparisons" to AbstractGoodGenericWhenComparisonsJvmDiagnosticTest::class,
    )
    testClass<AbstractNovaJvmDiagnosticTest> {
        model(
            "diagnostics",
            excludeDirs = listOf("helpers") + registryEntryComparisonParameterizedTests.keys.map {
                "registryEntryComparison/$it"
            }
        )
    }
    registryEntryComparisonParameterizedTests.forEach { [dir, klass] ->
        testClass(klass.java) {
            model("diagnostics/registryEntryComparison/$dir")
        }
    }
}