package xyz.xenondevs.nova.compiler

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory

internal object NovaDiagnostics : KtDiagnosticsContainer() {
    
    val REGISTRY_ENTRY_COMPARISON = diagnostic("NOVA_REGISTRY_ENTRY_COMPARISON")
    val MATERIAL_USAGE = diagnostic("NOVA_MATERIAL_USAGE")
    val KEY_TO_STRING = diagnostic("NOVA_KEY_TO_STRING")
    val TYPED_KEY_AS_KEY = diagnostic("NOVA_TYPED_KEY_AS_KEY")
    
    private fun diagnostic(name: String) = KtDiagnosticFactory0(
        name,
        Severity.WARNING,
        SourceElementPositioningStrategies.DEFAULT,
        Any::class,
        NovaDiagnosticMessages
    )
    
    override fun getRendererFactory(): BaseDiagnosticRendererFactory = NovaDiagnosticMessages
}

private object NovaDiagnosticMessages : BaseDiagnosticRendererFactory() {
    
    override val MAP by KtDiagnosticFactoryToRendererMap("NovaDiagnostics") { map ->
        map.put(
            NovaDiagnostics.REGISTRY_ENTRY_COMPARISON,
            "Nova: Do not compare RegistryEntry<T> with T. Compare entries or their values instead."
        )
        map.put(
            NovaDiagnostics.MATERIAL_USAGE,
            "Nova: Do not use Material. Use ItemType or BlockType instead."
        )
        map.put(
            NovaDiagnostics.KEY_TO_STRING,
            "Nova: Do not call Key.toString(), including through string interpolation. Use asString() instead."
        )
        map.put(
            NovaDiagnostics.TYPED_KEY_AS_KEY,
            "Nova: Do not use TypedKey as Key. Use key() instead."
        )
    }
}
