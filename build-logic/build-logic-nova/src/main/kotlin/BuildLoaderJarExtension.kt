import org.gradle.api.provider.Property

abstract class BuildLoaderJarExtension {
    
    abstract val gameVersion: Property<String>
    
}