import org.gradle.api.Action
import org.gradle.api.file.FileCopyDetails
import java.io.Serializable

class ExpandPropertiesAction(
    private val properties: Map<String, String>
) : Action<FileCopyDetails>, Serializable {
    
    override fun execute(file: FileCopyDetails) {
        file.expand(properties)
    }
    
}
