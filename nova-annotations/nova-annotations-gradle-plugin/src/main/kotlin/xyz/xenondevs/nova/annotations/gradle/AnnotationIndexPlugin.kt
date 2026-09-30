package xyz.xenondevs.nova.annotations.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import xyz.xenondevs.nova.annotations.AnnotationIndex

class AnnotationIndexPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        val extension = project.extensions.create("annotationIndex", AnnotationIndexExtension::class.java)
        val index = project.tasks.register("generateAnnotationIndex", GenerateAnnotationIndex::class.java) { task ->
            task.description = "Indexes configured class and method annotations."
            task.classAnnotations.set(extension.classAnnotations)
            task.methodAnnotations.set(extension.methodAnnotations)
            task.classes.from(extension.classes.asFileTree.matching { it.include("**/*.class") })
            task.outputFile.convention(project.layout.buildDirectory.file("annotations/${AnnotationIndex.FILE_NAME}"))
        }
        
        project.pluginManager.withPlugin("java") {
            val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
            extension.classes.convention(sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).map { it.output.classesDirs })
            project.tasks.named(JavaPlugin.JAR_TASK_NAME, Jar::class.java) { jar ->
                jar.from(index.flatMap { it.outputFile })
            }
        }
    }
    
}
