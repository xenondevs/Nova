package xyz.xenondevs.nova.compiler

import org.jetbrains.kotlin.compiler.plugin.devkit.generateDevKitTestsWithJUnit5

fun main() = generateDevKitTestsWithJUnit5 {
    testClass<AbstractNovaJvmDiagnosticTest> {
        model("diagnostics", excludeDirs = listOf("helpers"))
    }
}