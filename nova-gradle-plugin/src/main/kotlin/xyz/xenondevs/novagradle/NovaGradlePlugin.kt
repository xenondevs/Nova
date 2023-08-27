package xyz.xenondevs.novagradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.register
import xyz.xenondevs.novagradle.task.AddonMetadataExtension
import xyz.xenondevs.novagradle.task.AddonMetadataTask
import xyz.xenondevs.novagradle.task.GenerateLanguageFilesTask
import xyz.xenondevs.novagradle.task.GenerateWailaTexturesExtension
import xyz.xenondevs.novagradle.task.GenerateWailaTexturesTask
import xyz.xenondevs.novagradle.util.AddonUtils

class NovaGradlePlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        val addonExt = project.extensions.create<AddonMetadataExtension>("addon")
        val addonMetadataTask = project.tasks.register<AddonMetadataTask>("addonMetadata")
        val jarTask = addonExt.jarTask.orElse(project.tasks.getByName<Jar>("jar"))
        addonMetadataTask.configure {
            this.id.set(addonExt.id)
            this.addonName.set(addonExt.name)
            this.version.set(addonExt.version)
            this.novaVersion.set(addonExt.novaVersion)
            this.main.set(addonExt.main)
            this.authors.set(addonExt.authors)
            this.depend.set(addonExt.depend)
            this.softdepend.set(addonExt.softdepend)
            this.jarTask.set(jarTask)
        }
        jarTask.get().finalizedBy(addonMetadataTask)
        
        val wailaExt = project.extensions.create<GenerateWailaTexturesExtension>("generateWailaTextures")
        val wailaTask = project.tasks.register<GenerateWailaTexturesTask>("generateWailaTextures")
        wailaTask.configure {
            novaVersion.set(wailaExt.novaVersion.orElse(addonExt.novaVersion))
            resourcesDir.set(wailaExt.resourcesDir.orElse("src/main/resources/"))
            addonId.set(wailaExt.addonId.orElse(addonExt.id).orElse(resourcesDir.map { AddonUtils.getAddonId(project, it) }))
            filter.set(wailaExt.filter.orElse { true })
        }
        
        project.tasks.register<GenerateLanguageFilesTask>("generateLanguageFiles")
        
        val novaCfg = project.configurations.create("nova")
        project.configurations.getByName("implementation").extendsFrom(novaCfg)
    }
    
}