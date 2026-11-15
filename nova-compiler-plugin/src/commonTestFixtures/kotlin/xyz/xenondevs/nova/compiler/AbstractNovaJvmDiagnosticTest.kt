package xyz.xenondevs.nova.compiler

import org.jetbrains.kotlin.compiler.plugin.devkit.backendTestPhase
import org.jetbrains.kotlin.compiler.plugin.devkit.runners.DevKitJvmDiagnosticTest
import org.jetbrains.kotlin.compiler.plugin.devkit.services.configurePlugin
import org.jetbrains.kotlin.compiler.plugin.devkit.services.configureTestDataLibrary
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives.RUN_PIPELINE_TILL
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.bind
import xyz.xenondevs.nova.compiler.NovaHelpersProvider.Helper

abstract class AbstractNovaJvmDiagnosticTest : DevKitJvmDiagnosticTest({
    defaultDirectives {
        // Disable dumping FIR
        // Nova plugin consists solely of checkers, so FIR dumps aren't super relevant
        -FirDiagnosticsDirectives.FIR_DUMP
        RUN_PIPELINE_TILL with backendTestPhase
    }
    configurePlugin(NovaCompilerPluginRegistrar())
    forTestsMatching("*/typedKeyAsKey/*") {
        configureTestDataLibrary(TestDataLibraries.paperApi) { true }
    }
    forTestsMatching("*/registryEntryComparison/*") {
        configureTestDataLibrary(TestDataLibraries.paperApi) { true }
        configureTestDataLibrary(TestDataLibraries.novaRegistry) { true }
        useAdditionalSourceProviders(::NovaHelpersProvider.bind(listOf(Helper.NovaItem)))
    }
    forTestsMatching("*/materialUsage/*") {
        configureTestDataLibrary(TestDataLibraries.paperApi) { true }
        useAdditionalSourceProviders(::NovaHelpersProvider.bind(listOf(Helper.Material)))
    }
    forTestsMatching("*/keyToString/*") {
        configureTestDataLibrary(TestDataLibraries.paperApi) { true }
    }
})

class NovaHelpersProvider(testServices: TestServices, private val helpers: List<Helper>) :
    AdditionalSourceProvider(testServices) {
    
    companion object {
        const val DIAGNOSTICS_HELPERS = "diagnostics/helpers"
    }
    
    enum class Helper(val path: String) {
        NovaItem("$DIAGNOSTICS_HELPERS/NovaItem.kt"),
        Material("$DIAGNOSTICS_HELPERS/Material.kt"),
    }
    
    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure
    ): List<TestFile> {
        val classLoader = this::class.java.classLoader
        return helpers.map { classLoader.getResource(it.path)!!.toTestFile() }
    }
}
