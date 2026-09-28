import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

private const val COMPILER_PLUGIN_ID = "xyz.xenondevs.nova.compiler"
private const val COMPILER_PLUGIN_GROUP = "xyz.xenondevs.nova"
private const val COMPILER_PLUGIN_ARTIFACT = "nova-compiler-plugin"

internal class NovaCompilerPluginSupportPlugin : KotlinCompilerPluginSupportPlugin {
    
    private lateinit var novaVersion: String
    
    override fun apply(target: Project) {
        novaVersion = target.version.toString()
        target.configurations
            .named { it.startsWith("kotlinCompilerPluginClasspath") }
            .configureEach {
                resolutionStrategy.dependencySubstitution {
                    substitute(module("$COMPILER_PLUGIN_GROUP:$COMPILER_PLUGIN_ARTIFACT"))
                        .using(project(":nova-compiler-plugin"))
                }
            }
    }
    
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true
    
    override fun getCompilerPluginId(): String = COMPILER_PLUGIN_ID
    
    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(COMPILER_PLUGIN_GROUP, COMPILER_PLUGIN_ARTIFACT, novaVersion)
    
    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> =
        kotlinCompilation.target.project.providers.provider { emptyList() }
}
