package xyz.xenondevs.novagradle.util

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.jvm.tasks.Jar
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path

object TaskUtils {
    
    fun getOutputFile(jar: Jar): File {
        val dir = jar.destinationDirectory.get().asFile
        var name = listOf(
            jar.archiveBaseName.orNull ?: "",
            jar.archiveAppendix.orNull ?: "",
            jar.archiveVersion.orNull ?: "",
            jar.archiveClassifier.orNull ?: ""
        ).filterNot(String::isBlank).joinToString("-")
        jar.archiveExtension.orNull?.let { name += ".$it" }
        
        return File(dir, name)
    }
    
    fun findCompileTimeArtifact(project: Project, group: String, name: String): ResolvedArtifactResult {
        for (artifact in project.configurations.getByName("compileClasspath").incoming.artifacts.artifacts) {
            val id = (artifact.id.componentIdentifier as? ModuleComponentIdentifier)
                ?: continue
            
            if (id.group == group && id.module == name)
                return artifact
        }
        
        throw IllegalStateException("$group:$name not found in compileClasspath artifacts")
    }
    
    fun findNovaArtifact(project: Project): ResolvedArtifactResult =
        findCompileTimeArtifact(project, "xyz.xenondevs.nova", "nova")
    
    fun readNovaAndApiVersion(project: Project): Pair<String, String> {
        val artifact = findNovaArtifact(project)
        val id = artifact.id.componentIdentifier as ModuleComponentIdentifier
        return id.version to readApiVersion(artifact.file.toPath())
    }
    
    private fun readApiVersion(jarPath: Path): String {
        FileSystems.newFileSystem(jarPath).use { fs ->
            val pluginYml = YamlConfigurationLoader.builder()
                .path(fs.getPath("paper-plugin.yml"))
                .build()
                .load()
            
            return pluginYml.node("api-version").string
                ?: throw IllegalArgumentException("$jarPath contains no paper-plugin.yml with api-version")
        }
    }
    
}