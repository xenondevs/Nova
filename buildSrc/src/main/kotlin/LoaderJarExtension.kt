import org.gradle.api.provider.Property

abstract class LoaderJarExtension {
    
    abstract val spigotVersion: Property<String>
    
}