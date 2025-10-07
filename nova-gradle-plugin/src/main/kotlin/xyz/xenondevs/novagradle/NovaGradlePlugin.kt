package xyz.xenondevs.novagradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import xyz.xenondevs.commons.version.Version
import xyz.xenondevs.novagradle.task.AddonExtension
import xyz.xenondevs.novagradle.task.AddonJarTask
import xyz.xenondevs.novagradle.task.GenerateLanguageFilesTask
import xyz.xenondevs.novagradle.task.GenerateWailaTexturesExtension
import xyz.xenondevs.novagradle.task.GenerateWailaTexturesTask
import xyz.xenondevs.novagradle.task.PluginDependency
import xyz.xenondevs.novagradle.task.PluginDependency.Load
import xyz.xenondevs.novagradle.task.PluginDependency.Stage
import xyz.xenondevs.novagradle.util.TaskUtils
import xyz.xenondevs.origami.extension.OrigamiExtension
import xyz.xenondevs.origami.task.packaging.PrepareOrigamiMarkerTask

class NovaGradlePlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        project.pluginManager.apply("xyz.xenondevs.origami")
        
        val libraryLoaderCfg = project.configurations.create("libraryLoader")
        project.configurations.getByName("implementation").extendsFrom(libraryLoaderCfg)
        
        val addonExt = project.extensions.create<AddonExtension>("addon")
        val wailaExt = project.extensions.create<GenerateWailaTexturesExtension>("generateWailaTextures")
        
        project.extensions.getByName<OrigamiExtension>("origami").apply {
            devBundleVersion.set(Versions.PAPER)
            pluginId.set(addonExt.name.map { it.lowercase() })
            transitiveAccessWidenerSources.from(
                project.configurations.detachedConfiguration(
                    project.dependencyFactory.create("xyz.xenondevs.nova:nova:${Versions.NOVA}")
                )
            )
        }
        
        project.tasks.register<AddonJarTask>("addonJar") {
            group = LifecycleBasePlugin.BUILD_GROUP
            dependsOn("jar")
            
            addonName.set(addonExt.name)
            main.set(addonExt.main)
            pluginMain.set(addonExt.pluginMain)
            loader.set(addonExt.loader)
            bootstrapper.set(addonExt.bootstrapper)
            version.set(addonExt.version)
            dependencies.set(addonExt.dependencies)
            dependencies.add(PluginDependency("Nova", Stage.BOOTSTRAP, Load.BEFORE, true, true))
            dependencies.add(PluginDependency("Nova", Stage.SERVER, Load.BEFORE, true, true))
            addonDescription.set(addonExt.description)
            authors.set(addonExt.authors)
            contributors.set(addonExt.contributors)
            website.set(addonExt.website)
            prefix.set(addonExt.prefix)
            
            input.set(project.tasks.named<Jar>("jar").flatMap { it.archiveFile })
            origamiMarker.set(project.tasks.named<PrepareOrigamiMarkerTask>("_oriPrepareMarker").flatMap { it.jsonOutput })
            output.set(
                addonExt.destination
                    .orElse(project.layout.buildDirectory.dir("libs/"))
                    .map { dir ->
                        val name = addonExt.name.getOrElse(project.name)
                        val version = addonExt.version.getOrElse(project.version.toString())
                        val novaVersion = Version(TaskUtils.readNovaAndApiVersion(project).first)
                        
                        dir.file(addonExt.fileName.orNull ?: "$name-$version+Nova-${novaVersion.toString(omitIdx = 2, omitMetadata = true)}.jar")
                    }
            )
        }
        
        project.tasks.register<GenerateWailaTexturesTask>("generateWailaTextures") {
            resourcesDir.set(wailaExt.resourcesDir.orElse(project.layout.projectDirectory.dir("src/main/resources/")))
            addonId.set(addonExt.name.map { it.lowercase() })
            filter.set(wailaExt.filter.orElse { true })
        }
        
        project.tasks.register<GenerateLanguageFilesTask>("generateLanguageFiles")
    }
    
}