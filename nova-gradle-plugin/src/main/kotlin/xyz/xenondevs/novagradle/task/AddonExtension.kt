package xyz.xenondevs.novagradle.task

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.jvm.tasks.Jar

abstract class AddonExtension {
    
    abstract val id: Property<String>
    abstract val name: Property<String>
    abstract val version: Property<String>
    abstract val novaVersion: Property<String>
    abstract val main: Property<String>
    abstract val authors: ListProperty<String>
    abstract val depend: ListProperty<String>
    abstract val softdepend: ListProperty<String>
    abstract val jarTask: Property<Jar>
    
}