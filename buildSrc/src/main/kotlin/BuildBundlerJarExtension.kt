import org.gradle.api.provider.Property

abstract class BuildBundlerJarExtension {
    
    abstract val gameVersion: Property<String>
    
}