package xyz.xenondevs.nova.compiler

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val WARNING_MARKER = Regex("// warn(?: (\\d+))?")

internal abstract class CompilerPluginTest(
    private val diagnosticName: String,
    private vararg val stubs: Pair<String, String>
) {
    
    @TempDir
    lateinit var tempDir: Path
    
    /**
     * Each `// warn` marks a line that must produce this inspection's warning.
     * Use `// warn N` when multiple expressions on the same line must be reported.
     * Unmarked lines must not produce any Nova diagnostics.
     */
    protected fun assertDiagnostics(source: String) {
        val code = source.trimIndent()
        val expectedLines = code.lines().flatMapIndexed { index, line ->
            val marker = WARNING_MARKER.find(line) ?: return@flatMapIndexed emptyList()
            val count = marker.groupValues[1].toIntOrNull() ?: 1
            List(count) { index + 1 }
        }
        val sourceDir = Files.createTempDirectory(tempDir, "sources")
        val outputDir = Files.createTempDirectory(tempDir, "output")
        val sourceFiles = (stubs.toList() + ("Test.kt" to code)).map { [name, content] ->
            sourceDir.resolve(name).apply { writeText(content.trimIndent()) }.toString()
        }
        val messages = mutableListOf<CompilerMessage>()
        val collector = object : MessageCollector {
            override fun clear() = messages.clear()
            override fun hasErrors(): Boolean = messages.any { it.severity.isError }
            override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
                messages += CompilerMessage(severity, message, location)
            }
        }
        val arguments = K2JVMCompilerArguments().apply {
            freeArgs = sourceFiles
            destination = outputDir.toString()
            classpath = Unit::class.java.protectionDomain.codeSource.location.toURI().path
            jdkHome = System.getProperty("java.home")
            pluginClasspaths = arrayOf(checkNotNull(System.getProperty("nova.compiler.plugin.jar")))
            noStdlib = true
            noReflect = true
            renderInternalDiagnosticNames = true
        }
        val exitCode = K2JVMCompiler().exec(collector, Services.EMPTY, arguments)
        val output = messages.filter { it.severity != CompilerMessageSeverity.LOGGING }.joinToString("\n")
        assertEquals(ExitCode.OK, exitCode, output)
        val diagnostics = messages.filter {
            it.location?.path?.replace('\\', '/')?.endsWith("/Test.kt") == true && it.message.startsWith("[NOVA_")
        }
        assertEquals(expectedLines, diagnostics.map { it.location!!.line }.sorted(), output)
        assertTrue(diagnostics.all { it.severity == CompilerMessageSeverity.WARNING }, output)
        assertTrue(diagnostics.all { it.message.startsWith("[$diagnosticName] ") }, output)
    }
    
    private data class CompilerMessage(
        val severity: CompilerMessageSeverity,
        val message: String,
        val location: CompilerMessageSourceLocation?
    )
}
