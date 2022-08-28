package xyz.xenondevs.novagradle.util

import org.gradle.jvm.tasks.Jar
import java.io.File

object TaskUtils {
    
    fun getOutputFile(jar: Jar): File {
        val dir = jar.destinationDirectory.get().asFile
        var name = listOf(
            jar.archiveBaseName.orNull ?: "",
            jar.archiveAppendix.orNull ?: "",
            jar.archiveVersion.orNull ?: "",
            jar.archiveClassifier.orNull ?: ""
        ).filterNot(String::isBlank).joinToString("-")
        jar.archiveExtension.orNull?.let { name += ".$it" }
        
        return File(dir, name)
    }
    
}