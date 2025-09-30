import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

abstract class BuildBundlerJarExtension {
    
    abstract val gameVersion: Property<String>
    
    abstract val novaInput: RegularFileProperty
    abstract val input: ConfigurableFileCollection
    
}