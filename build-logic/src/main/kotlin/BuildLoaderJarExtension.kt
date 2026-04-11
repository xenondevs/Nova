
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property

abstract class BuildLoaderJarExtension {
    
    abstract val gameVersion: Property<String>
    abstract val merge: ConfigurableFileCollection
    
}