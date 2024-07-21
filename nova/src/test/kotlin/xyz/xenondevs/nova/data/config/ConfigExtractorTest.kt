package xyz.xenondevs.nova.data.config

import com.google.common.jimfs.Jimfs
import org.junit.jupiter.api.Test
import xyz.xenondevs.commons.provider.mutable.mutableProvider
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.readText
import kotlin.test.assertEquals

class ConfigExtractorTest {
    
    @Test
    fun `test initial extraction`() {
        val source = source("internal-config-original.yml")
        val dest = dest()
        
        ConfigExtractor(mutableProvider(HashMap()))
            .extract("config.yml", source, dest)
        
        assertConfigEquals(source, dest)
    }
    
    @Test
    fun `test removal of entries and comments on user-unmodified config`() {
        val sourceOriginal = source("internal-config-original.yml")
        val sourceStripped = source("internal-config-stripped-entries.yml")
        val dest = dest()
        
        val extractor = ConfigExtractor(mutableProvider(HashMap()))
        extractor.extract("config.yml", sourceOriginal, dest)
        extractor.extract("config.yml", sourceStripped, dest)
        
        assertConfigEquals(sourceStripped, dest)
    }
    
    @Test
    fun `test adding entries and comments on user-unmodified config`() {
        val sourceOriginal = source("internal-config-original.yml")
        val sourceAdded = source("internal-config-added-entries.yml")
        val dest = dest()
        
        val extractor = ConfigExtractor(mutableProvider(HashMap()))
        extractor.extract("config.yml", sourceOriginal, dest)
        extractor.extract("config.yml", sourceAdded, dest)
        
        assertConfigEquals(sourceAdded, dest)
    }
    
    @Test
    fun `test adding, removing, changing entries and comments on user-unmodified config`() {
        val sourceOriginal = source("internal-config-original.yml")
        val sourceChanged = source("internal-config-changed-entries.yml")
        val dest = dest()
        
        val extractor = ConfigExtractor(mutableProvider(HashMap()))
        extractor.extract("config.yml", sourceOriginal, dest)
        extractor.extract("config.yml", sourceChanged, dest)
        
        assertConfigEquals(sourceChanged, dest)
    }
    
    @Test
    fun `test adding, removing, changing entries and comments on a user-modified config that also added, removed, and changed entries`()  {
        val sourceOriginal = source("internal-config-original.yml")
        val sourceChanged = source("internal-config-changed-entries.yml")
        val dest = dest()
        
        val extractor = ConfigExtractor(mutableProvider(HashMap()))
        extractor.extract("config.yml", sourceOriginal, dest)
        source("server-config-changed-entries.yml").copyTo(dest, true) // simulate user changes
        extractor.extract("config.yml", sourceChanged, dest)
        
        assertConfigEquals(source("server-config-updated-expected.yml"), dest)
    }
    
    private fun source(path: String): Path =
        Path.of(javaClass.getResource("/configs/$path")!!.toURI())
    
    private fun dest(): Path =
        Jimfs.newFileSystem().rootDirectories.first().resolve("out.yml")
    
    private fun assertConfigEquals(expected: Path, actual: Path) {
        assertEquals(
            expected.readText().replace("\r", ""),
            actual.readText().replace("\r", "")
        )
    }
    
}