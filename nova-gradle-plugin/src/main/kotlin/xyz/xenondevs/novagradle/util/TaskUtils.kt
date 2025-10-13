package xyz.xenondevs.novagradle.util

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult

internal object TaskUtils {
    
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
    
}