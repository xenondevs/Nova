
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

abstract class BuildBundlerJarTask : DefaultTask() {
    
    @get:Input
    abstract var nova: Project
    
    @get:Input
    abstract var novaApi: Project
    
    @get:Input
    abstract var hooks: List<Project>
    
    private val buildDir = project.layout.buildDirectory.asFile.get()
    
    @TaskAction
    fun run() {
        val novaFile = createLoaderJar()
        
        // copy to custom output directory
        val customOutDir = (project.findProperty("outDir") as? String)?.let(::File)
            ?: System.getProperty("outDir")?.let(::File)
            ?: return
        customOutDir.mkdirs()
        val copyTo = File(customOutDir, novaFile.name)
        novaFile.inputStream().use { ins -> copyTo.outputStream().use { out -> ins.copyTo(out) } }
    }
    
    private fun createLoaderJar(): File {
        buildDir.mkdirs()
        
        val jar = buildDir.resolve("Nova-${project.version}.jar")
        ZipOutputStream(jar.outputStream()).use { out ->
            include(out, hooks + nova + novaApi)
            
            // include dependencies
            val runtimeArtifacts = nova.configurations.getByName("mojangMappedServerRuntime").incoming.artifacts.artifacts
                .mapNotNullTo(HashSet()) { (it.id.componentIdentifier as? ModuleComponentIdentifier)?.moduleIdentifier }
            nova.configurations.getByName("novaLoader").incoming.artifacts.artifacts
                .asSequence()
                .filter { (it.id.componentIdentifier as ModuleComponentIdentifier).moduleIdentifier !in runtimeArtifacts }
                .forEach { artifact ->
                    val file = artifact.file
                    val id = artifact.id.componentIdentifier as ModuleComponentIdentifier
                    val path = "lib/" + id.group.replace('.', '/') + "/" + id.module + "/" + id.version + "/" + file.name
                    out.putNextEntry(ZipEntry(path))
                    file.inputStream().use { inp -> inp.transferTo(out) }
                }
        }
        
        return jar
    }
    
    private fun include(out: ZipOutputStream, projects: List<Project>) {
        projects.forEach { project ->
            iterateClasses(project) { file, path ->
                out.putNextEntry(ZipEntry(path))
                file.inputStream().use { inp -> inp.transferTo(out) }
            }
            iterateResources(project) { file, path ->
                out.putNextEntry(ZipEntry(path))
                file.inputStream().use { inp -> inp.transferTo(out) }
            }
        }
    }
    
    private fun iterateClasses(project: Project, run: (File, String) -> Unit) {
        val kotlinClasses = project.layout.buildDirectory.asFile.get().resolve("classes/kotlin/main")
        val javaClasses = project.layout.buildDirectory.asFile.get().resolve("classes/java/main")
        iterateClasses(kotlinClasses, run)
        iterateClasses(javaClasses, run)
    }
    
    private fun iterateClasses(dir: File, run: (File, String) -> Unit) {
        dir.walkTopDown()
            .filter { it.isFile }
            .map { it to it.relativeTo(dir).invariantSeparatorsPath }
            .filter { (_, path) -> !path.startsWith("META-INF") }
            .forEach { (file, path) -> run(file, path) }
    }
    
    private fun iterateResources(project: Project, run: (File, String) -> Unit) {
        val resources = project.layout.buildDirectory.asFile.get().resolve("resources/main")
        resources.walkTopDown()
            .filter { it.isFile }
            .forEach {
                val path = it.relativeTo(resources).invariantSeparatorsPath
                run(it, path)
            }
    }
    
}