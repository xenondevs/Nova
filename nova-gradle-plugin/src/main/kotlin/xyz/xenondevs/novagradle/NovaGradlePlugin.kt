package xyz.xenondevs.novagradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.register
import xyz.xenondevs.novagradle.task.AddonExtension
import xyz.xenondevs.novagradle.task.AddonJarTask
import xyz.xenondevs.novagradle.task.GenerateLanguageFilesTask
import xyz.xenondevs.novagradle.task.GenerateWailaTexturesExtension
import xyz.xenondevs.novagradle.task.GenerateWailaTexturesTask
import xyz.xenondevs.novagradle.task.PluginDependency
import xyz.xenondevs.novagradle.task.PluginDependency.Load
import xyz.xenondevs.novagradle.task.PluginDependency.Stage
import xyz.xenondevs.novagradle.util.TaskUtils

class NovaGradlePlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        val libraryLoaderCfg = project.configurations.create("libraryLoader")
        project.configurations.getByName("implementation").extendsFrom(libraryLoaderCfg)
        
        val addonExt = project.extensions.create<AddonExtension>("addon")
        val addonJarTask = project.tasks.register<AddonJarTask>("addonJar") {
            group = "build"
            dependsOn("jar")
        }
        addonJarTask.configure {
            this.addonName.set(addonExt.name)
            this.main.set(addonExt.main)
            this.loader.set(addonExt.loader)
            this.bootstrapper.set(addonExt.bootstrapper)
            this.version.set(addonExt.version)
            this.dependencies.set(addonExt.dependencies)
            this.dependencies.add(PluginDependency("Nova", Stage.BOOTSTRAP, Load.BEFORE, true, true))
            this.dependencies.add(PluginDependency("Nova", Stage.SERVER, Load.BEFORE, true, true))
            this.addonDescription.set(addonExt.description)
            this.authors.set(addonExt.authors)
            this.contributors.set(addonExt.contributors)
            this.website.set(addonExt.website)
            this.prefix.set(addonExt.prefix)
            
            this.input.set(TaskUtils.getOutputFile(project.tasks.getByName<Jar>("jar")))
            this.output.set(
                addonExt.destination
                    .orElse(project.layout.buildDirectory.dir("libs/"))
                    .map {
                        val name = addonExt.name.getOrElse(project.name)
                        val version = addonExt.version.getOrElse(project.version.toString())
                        it.file("$name-$version.jar") 
                    }
            )
        }
        
        val wailaExt = project.extensions.create<GenerateWailaTexturesExtension>("generateWailaTextures")
        val wailaTask = project.tasks.register<GenerateWailaTexturesTask>("generateWailaTextures")
        wailaTask.configure {
            this.resourcesDir.set(wailaExt.resourcesDir.orElse("src/main/resources/"))
            this.addonId.set(addonExt.name.map { it.lowercase() })
            this.filter.set(wailaExt.filter.orElse { true })
        }
        
        project.tasks.register<GenerateLanguageFilesTask>("generateLanguageFiles")
    }
    
    
}