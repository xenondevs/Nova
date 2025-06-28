package xyz.xenondevs.novadokka

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement

class NovaDokkaPlugin : DokkaPlugin() {
    
    val hideInternalApiTransformer by extending {
        plugin<DokkaBase>().preMergeDocumentableTransformer providing ::HideInternalApiTransformer
    }
    
    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement() = PluginApiPreviewAcknowledgement
    
}