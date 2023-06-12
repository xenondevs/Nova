package xyz.xenondevs.novagradle.task

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import org.bukkit.configuration.file.YamlConfiguration
import org.gradle.api.DefaultTask
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.internal.artifacts.repositories.DefaultMavenArtifactRepository
import org.gradle.api.internal.artifacts.repositories.DefaultMavenLocalArtifactRepository
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import xyz.xenondevs.novagradle.util.TaskUtils

private const val MAVEN_CENTRAL = "https://repo1.maven.org/maven2/"

abstract class AddonTask : DefaultTask() {
    
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
        val addonCfg = YamlConfiguration()
        configureAddonValues(addonCfg)
        generateLibrariesYaml(addonCfg)
        
        val file = TaskUtils.getOutputFile(jarTask.get())
        val zip = ZipFile(file)
        zip.addStream(
            addonCfg.saveToString().byteInputStream(),
            ZipParameters().apply { fileNameInZip = "addon.yml" }
        )
    }
    
    private fun configureAddonValues(cfg: YamlConfiguration) {
        cfg["id"] = id.get()
        cfg["name"] = addonName.get()
        cfg["version"] = version.get()
        cfg["novaVersion"] = novaVersion.get()
        cfg["main"] = main.get()
        
        cfg["authors"] = authors.get()
        cfg["depend"] = depend.get()
        cfg["softdepend"] = softdepend.get()
    }
    
    @Suppress("SENSELESS_COMPARISON") // it isn't
    private fun generateLibrariesYaml(cfg: YamlConfiguration) {
        cfg["repositories"] = project.repositories.asSequence()
            .filterIsInstance<DefaultMavenArtifactRepository>()
            .filter { it !is DefaultMavenLocalArtifactRepository }
            .mapTo(HashSet()) { it.url.toString() }
            .apply { this -= MAVEN_CENTRAL }
            .toList() // Required for proper serialization
        
        cfg["libraries"] = project.configurations.getByName("nova")
            .incoming.dependencies.asSequence()
            .filterIsInstance<DefaultExternalModuleDependency>()
            .mapTo(ArrayList()) { dep ->
                val coords = getArtifactCoords(dep)
                val excludeRules = dep.excludeRules
                if (excludeRules.isNotEmpty()) {
                    val exCfg = YamlConfiguration()
                    exCfg["library"] = coords
                    exCfg["exclusions"] = excludeRules.map {
                        require(it.group != null && it.module != null) { "Exclusion rules need to specify group and module" }
                        "${it.group}:${it.module}::jar"
                    }
                    
                    return@mapTo exCfg
                }
                
                return@mapTo coords
            }
    }
    
    private fun getArtifactCoords(dependency: DefaultExternalModuleDependency): String {
        val artifact = dependency.artifacts.firstOrNull()
        return if (artifact != null)
            "${dependency.group}:${dependency.name}:${artifact.extension}:${artifact.classifier}:${dependency.version}"
        else "${dependency.group}:${dependency.name}:${dependency.version}"
    }
    
}
