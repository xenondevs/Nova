package xyz.xenondevs.novagradle.task

import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.property
import xyz.xenondevs.novagradle.Versions
import xyz.xenondevs.novagradle.task.PluginDependency.Load
import xyz.xenondevs.novagradle.task.PluginDependency.Stage
import java.io.Serializable
import javax.inject.Inject

/**
 * Contains configuration options for the `addonJar` task.
 */
abstract class AddonExtension @Inject constructor(
    project: Project,
    objects: ObjectFactory
) {
    
    /**
     * The name of the addon. The lowercase version is used as the addon's id (namespace).
     */
    abstract val name: Property<String>
    
    /**
     * The version of the addon. This is parsed using
     * [xenondevs' commons-version](https://commons.dokka.xenondevs.xyz/commons-version/xyz.xenondevs.commons.version/-version/index.html)
     * and needs to be valid according to that.
     */
    abstract val version: Property<String>
    
    /**
     * The path to the addons main class, e.g. `com.example.MyAddon`.
     * The main class needs to be a singleton object extending `xyz.xenondevs.nova.addon.Addon`.
     */
    abstract val main: Property<String>
    
    /**
     * (optional)
     * The path to the [JavaPlugin](https://docs.papermc.io/paper/dev/project-setup/#the-main-class) implementation.
     *
     * If left unset, a plugin main class will be generated.
     * The plugin instance can then still be accessed through your addon instance.
     */
    abstract val pluginMain: Property<String>
    
    /**
     * (optional)
     * The path to the [PluginBootstrap](https://docs.papermc.io/paper/dev/getting-started/paper-plugins/#bootstrapper) implementation.
     * (e.g. `com.example.MyBootstrapper`)
     */
    abstract val bootstrapper: Property<String>
    
    /**
     * (optional)
     * The path to the [PluginLoader](https://docs.papermc.io/paper/dev/getting-started/paper-plugins/#loaders) implementation.
     *
     * If left unset, a plugin loader that downloads all libraries from the `libraryLoader` dependency configuration will be generated.
     * Otherwise, the instruction to download the `libraryLoader` dependencies will be injected into the specified loader class.
     */
    abstract val loader: Property<String>
    
    /**
     * (optional)
     * A list of [plugin dependencies](https://docs.papermc.io/paper/dev/getting-started/paper-plugins/#dependency-declaration).
     *
     * @see dependency
     */
    abstract val dependencies: ListProperty<PluginDependency>
    
    /**
     * (optional)
     * A [description](https://docs.papermc.io/paper/dev/plugin-yml/#description) of the addon.
     */
    abstract val description: Property<String>
    
    /**
     * (optional)
     * A [list of authors](https://docs.papermc.io/paper/dev/plugin-yml/#author--authors) of the addon.
     */
    abstract val authors: ListProperty<String>
    
    /**
     * (optional)
     * A [list of contributors](https://docs.papermc.io/paper/dev/plugin-yml/#contributors) of the addon.
     */
    abstract val contributors: ListProperty<String>
    
    /**
     * (optional)
     * The [website](https://docs.papermc.io/paper/dev/plugin-yml/#website) of the addon.
     */
    abstract val website: Property<String>
    
    /**
     * (optional)
     * The [prefix](https://docs.papermc.io/paper/dev/plugin-yml/#prefix) of the addon.
     * Used for logging.
     */
    abstract val prefix: Property<String>
    
    /**
     * The [CopySpec] to merge into `addonJar`.
     *
     * Defaults to the `jar` task.
     */
    val input: Property<CopySpec> = objects.property<CopySpec>()
        .convention(project.tasks.named<Jar>("jar"))
    
    /**
     * The destination directory of the `addonJar` task.
     * 
     * Defaults to `build/libs`.
     */
    val destination: DirectoryProperty = objects.directoryProperty()
        .convention(project.layout.buildDirectory.dir("libs"))
    
    /**
     * The file name of the jar produced by the `addonJar` task.
     * 
     * Defaults to `<name>-<version>+Nova-<novaRelease>.jar` (e.g. `MyAddon-1.0.0+Nova-0.21.jar`).
     */
    val fileName: Property<String> = objects.property<String>()
        .convention(name.zip(version) { name, version -> "$name-$version+Nova-${Versions.NOVA_RELEASE}.jar"})
    
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

/**
 * A [dependency](https://docs.papermc.io/paper/dev/getting-started/paper-plugins/#dependency-declaration) on another plugin / addon.
 */
data class PluginDependency(
    /**
     * The name of the plugin this dependency refers to.
     */
    val name: String,
    
    /**
     * The stage in which the dependency exists.
     */
    val stage: Stage,
    
    /**
     * Whether the dependency should be loaded before or after your addon.
     */
    val load: Load,
    
    /**
     * Whether the dependency is required for your addon to load.
     */
    val required: Boolean,
    
    /**
     * Whether your addon should have access to the dependency's classes.
     */
    val joinClasspath: Boolean
) : Serializable {
    
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