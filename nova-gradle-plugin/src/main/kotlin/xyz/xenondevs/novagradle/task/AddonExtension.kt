package xyz.xenondevs.novagradle.task

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import xyz.xenondevs.novagradle.task.PluginDependency.Load
import xyz.xenondevs.novagradle.task.PluginDependency.Stage
import java.io.Serializable

abstract class AddonExtension {
    
    abstract val name: Property<String>
    abstract val version: Property<String>
    abstract val main: Property<String>
    abstract val bootstrapper: Property<String>
    abstract val loader: Property<String>
    abstract val dependencies: ListProperty<PluginDependency>
    abstract val description: Property<String>
    abstract val authors: ListProperty<String>
    abstract val contributors: ListProperty<String>
    abstract val website: Property<String>
    abstract val prefix: Property<String>
    abstract val destination: DirectoryProperty
    
    /**
     * Creates a dependency on a plugin named [name] in both the [Stage.BOOTSTRAP] and [Stage.SERVER] stages.
     * The load order is defined by [load], the plugin will require the dependency if [required] is true, and the plugin's
     * classes will be accessible if [joinClasspath] is true.
     */
    fun dependency(name: String, load: Load = Load.OMIT, required: Boolean = true, joinClasspath: Boolean = true) {
        dependency(name, Stage.BOOTSTRAP, load, required, joinClasspath)
        dependency(name, Stage.SERVER, load, required, joinClasspath)
    }
    
    /**
     * Creates a dependency on a plugin named [name] in the specified [stage].
     * The load order is defined by [load], the plugin will require the dependency if [required] is true, and the plugin's
     * classes will be accessible if [joinClasspath] is true.
     */
    fun dependency(name: String, stage: Stage, load: Load = Load.OMIT, required: Boolean = true, joinClasspath: Boolean = true) {
        dependencies.add(PluginDependency(name, stage, load, required, joinClasspath))
    }
    
}

data class PluginDependency(val name: String, val stage: Stage, val load: Load, val required: Boolean, val joinClasspath: Boolean) : Serializable {
    
    /**
     * The stage in which the dependency exists.
     */
    enum class Stage {
        BOOTSTRAP, SERVER
    }
    
    /**
     * Defines the load order of the dependency and your plugin.
     */
    enum class Load {
        
        /**
         * The dependency will be loaded before your plugin.
         */
        BEFORE,
        
        /**
         * The dependency will be loaded after your plugin.
         */
        AFTER,
        
        /**
         * The load order is unspecified.
         */
        OMIT
    }
    
}