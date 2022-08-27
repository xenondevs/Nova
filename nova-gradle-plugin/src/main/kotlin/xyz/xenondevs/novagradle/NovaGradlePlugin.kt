package xyz.xenondevs.novagradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import xyz.xenondevs.novagradle.task.GenerateWailaTexturesExtension
import xyz.xenondevs.novagradle.task.GenerateWailaTexturesTask
import xyz.xenondevs.novagradle.util.AddonUtils

class NovaGradlePlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        val wailaExt = project.extensions.create<GenerateWailaTexturesExtension>("generateWailaTextures")
        val wailaTask = project.tasks.register<GenerateWailaTexturesTask>("generateWailaTextures")
        
        wailaTask.configure { 
            novaVersion.set(wailaExt.novaVersion)
            resourcesDir.set(wailaExt.resourcesDir.orElse("src/main/resources/"))
            addonId.set(wailaExt.addonId.orElse(AddonUtils.getAddonId(project, resourcesDir.get())))
            filter.set(wailaExt.filter.orElse { true })
        }
    }
    
}