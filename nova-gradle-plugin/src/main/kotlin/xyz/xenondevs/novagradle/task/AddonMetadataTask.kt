package xyz.xenondevs.novagradle.task

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.artifacts.repositories.DefaultMavenLocalArtifactRepository
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier
import org.gradle.jvm.tasks.Jar
import xyz.xenondevs.commons.gson.toJsonArray
import xyz.xenondevs.commons.gson.writeToFile
import xyz.xenondevs.novagradle.util.TaskUtils
import java.nio.file.FileSystems

abstract class AddonMetadataTask : DefaultTask() {
    
    @get:Input
    abstract val id: Property<String>
    
    @get:Input
    abstract val addonName: Property<String>
    
    @get:Input
    abstract val version: Property<String>
    
    @get:Input
    abstract val novaVersion: Property<String>
    
    @get:Input
    abstract val main: Property<String>
    
    @get:Input
    abstract val authors: ListProperty<String>
    
    @get:Input
    abstract val depend: ListProperty<String>
    
    @get:Input
    abstract val softdepend: ListProperty<String>
    
    @get:Input
    abstract val jarTask: Property<Jar>
    
    @TaskAction
    fun run() {
        FileSystems.newFileSystem(TaskUtils.getOutputFile(jarTask.get()).toPath()).use { 
            val root = it.rootDirectories.first()
            
            val addonMetadataFile = root.resolve("addon_metadata.json")
            createPluginMetadata().writeToFile(addonMetadataFile)
            
            val addonLibrariesFile = root.resolve("addon_libraries.json")
            createLibrariesJson(project, "nova", emptyList()).writeToFile(addonLibrariesFile)
        }
    }
    
    private fun createPluginMetadata(): JsonObject {
        return JsonObject().apply {
            addProperty("id", id.get())
            addProperty("name", addonName.get())
            addProperty("version", version.get())
            addProperty("nova_version", novaVersion.get())
            addProperty("main", main.get())
            add("authors", authors.get().toJsonArray())
            add("depend", depend.get().toJsonArray())
            add("softdepend", softdepend.get().toJsonArray())
        }
    }
    
    @Suppress("SENSELESS_COMPARISON") // falsely interpreted as non-nullable
    private fun createLibrariesJson(project: Project, configurationName: String, excludedConfigurationNames: List<String>): JsonObject {
        val json = JsonObject()
        
        // repositories
        val repositories = JsonArray().also { json.add("repositories", it) }
        project.repositories.asSequence()
            .filterIsInstance<MavenArtifactRepository>()
            .filter { it !is DefaultMavenLocalArtifactRepository }
            .mapTo(LinkedHashSet()) { it.url.toString() }
            .forEach(repositories::add)
        
        // libraries and exclusions
        val libraries = JsonArray().also { json.add("libraries", it) }
        val configuration = project.configurations.getByName(configurationName)
        
        fun writeExcludeRules(rules: Set<ExcludeRule>, excludes: JsonArray) {
            rules.forEach {
                require(it.group != null && it.module != null) { "Exclusion rules need to specify group and module" }
                excludes.add("${it.group}:${it.module}")
            }
        }
        
        configuration.incoming.dependencies.asSequence()
            .filterIsInstance<ExternalModuleDependency>()
            .forEach { dependency ->
                val library = JsonObject().also(libraries::add)
                
                library.addProperty("coords", getArtifactCoords(dependency))
                val excludeRules = dependency.excludeRules
                writeExcludeRules(excludeRules, JsonArray().also { library.add("excludes", it) })
            }
        
        val excludes = JsonArray().also { json.add("excludes", it) }
        writeExcludeRules(configuration.excludeRules, excludes)
        for (exclusionConfigurationName in excludedConfigurationNames) {
            project.configurations.getByName(exclusionConfigurationName)
                .incoming.artifacts.artifacts.asSequence()
                .map { it.variant.owner }
                .filterIsInstance<DefaultModuleComponentIdentifier>()
                .forEach { excludes.add("${it.group}:${it.module}") }
        }
        
        return json
    }
    
    private fun getArtifactCoords(dependency: ExternalModuleDependency): String {
        val artifact = dependency.artifacts.firstOrNull()
        return if (artifact != null)
            "${dependency.group}:${dependency.name}:${artifact.extension}:${artifact.classifier}:${dependency.version}"
        else "${dependency.group}:${dependency.name}:${dependency.version}"
    }
    
}
