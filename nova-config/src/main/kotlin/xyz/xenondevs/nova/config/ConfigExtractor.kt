package xyz.xenondevs.nova.config

import net.kyori.adventure.key.Key
import org.snakeyaml.engine.v2.api.DumpSettings
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.api.YamlOutputStreamWriter
import org.snakeyaml.engine.v2.composer.Composer
import org.snakeyaml.engine.v2.emitter.Emitter
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.nodes.ScalarNode
import org.snakeyaml.engine.v2.nodes.SequenceNode
import org.snakeyaml.engine.v2.parser.ParserImpl
import org.snakeyaml.engine.v2.scanner.StreamReader
import org.snakeyaml.engine.v2.serializer.Serializer
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.jvm.optionals.getOrNull

/**
 * Utility for copying config templates to a destination file, adding and removing entries and comments as necessary
 * while keeping changes made by users intact.
 * 
 * @param extractedConfigs Storage that keeps track of what the config extractor extracted and what changes were made
 * by users, as to not overwrite them. This storage must persist across usages.
 */
class ConfigExtractor(private val extractedConfigs: MutableMap<Key, String>) {
    
    /**
     * Extracts the config identified by [configId] found at [source] to [dest].
     */
    fun extract(configId: Key, source: Path, dest: Path) {
        val internalCfg = loadYaml(source)
            ?: throw IllegalArgumentException("In-zip config cannot be empty")
        val extractedCfg = extractedConfigs[configId]?.let(::loadYaml)
        
        var serverCfg = dest.takeIf(Path::exists)?.let(::loadYaml)
        if (serverCfg == null || extractedCfg == null) {
            serverCfg = internalCfg
            extractedConfigs[configId] = writeYaml(serverCfg, false)
        } else {
            updateExistingConfig(serverCfg, extractedCfg, internalCfg)
            extractedConfigs[configId] = writeYaml(extractedCfg, false)
        }
        
        writeYaml(serverCfg, dest)
    }
    
    private fun updateExistingConfig(serverCfg: Node, extractedCfg: Node, internalCfg: Node) {
        var previousPath: List<String> = listOf("")
        internalCfg.walk { path, internalKeyNode, internalValueNode ->
            // for ordering, determine the entry name above (if present)
            val aboveEntry: String =
                if (previousPath.size >= path.size && previousPath.subList(0, path.size - 1) == path.subList(0, path.size - 1))
                    previousPath[path.size - 1]
                else ""
            
            val serverKeyValueNodes = serverCfg.get(path)
            var skipChildren = false
            if (serverKeyValueNodes == null) {
                // add new key
                serverCfg.set(path, internalValueNode, aboveEntry)
                extractedCfg.set(path, internalValueNode)
                
                // we don't need to explore this, we just added it
                skipChildren = true
            } else if (internalValueNode is ScalarNode || internalValueNode is SequenceNode) {
                // update value of scalar/sequence node (which cannot be walked down further), if unchanged by user
                val serverValueNode = serverKeyValueNodes.second
                val extractedValueNode = extractedCfg.get(path)?.second
                if (!internalValueNode.deepEquals(serverKeyValueNodes.second) && extractedValueNode.deepEquals(serverValueNode)) {
                    serverCfg.set(path, internalValueNode, aboveEntry)
                    extractedCfg.set(path, internalValueNode)
                }
            }
            
            // update comments
            serverCfg.get(path)!!.also { (serverKeyNode, serverValueNode) ->
                serverKeyNode.blockComments = internalKeyNode.blockComments
                serverKeyNode.inLineComments = internalKeyNode.inLineComments
                serverKeyNode.endComments = internalKeyNode.endComments
                
                serverValueNode.blockComments = internalValueNode.blockComments
                serverValueNode.inLineComments = internalValueNode.inLineComments
                serverValueNode.endComments = internalValueNode.endComments
            }
            
            previousPath = path
            if (skipChildren) NodeWalkDecision.SKIP else NodeWalkDecision.CONTINUE
        }
        
        // remove entries that were once extracted but are no longer in the internal config
        serverCfg.walk { path, _, _ ->
            if (internalCfg.get(path) == null && extractedCfg.get(path) != null) {
                serverCfg.remove(path)
                extractedCfg.remove(path)
            }
            NodeWalkDecision.CONTINUE
        }
    }
    
    private fun loadYaml(s: String): Node? {
        return loadYaml(s.byteInputStream())
    }
    
    private fun loadYaml(file: Path): Node? {
        return file.inputStream().use { inp -> loadYaml(inp) }
    }
    
    private fun loadYaml(inp: InputStream): Node? {
        val settings = LoadSettings.builder()
            .setParseComments(true)
            .build()
        val reader = StreamReader(settings, inp.reader())
        val parser = ParserImpl(settings, reader)
        val composer = Composer(settings, parser)
        return composer.singleNode.getOrNull()
    }
    
    private fun writeYaml(node: Node, comments: Boolean = true): String {
        val out = ByteArrayOutputStream()
        writeYaml(node, out, comments)
        return out.toString(Charsets.UTF_8)
    }
    
    private fun writeYaml(node: Node, file: Path, comments: Boolean = true) {
        file.parent.createDirectories()
        file.outputStream().use { out -> writeYaml(node, out, comments) }
    }
    
    private fun writeYaml(node: Node, out: OutputStream, comments: Boolean = true) {
        val settings = DumpSettings.builder()
            .setDumpComments(comments)
            .build()
        
        val writer = object : YamlOutputStreamWriter(out, Charsets.UTF_8) {
            override fun processIOException(e: IOException) {
                throw e
            }
        }
        val emitter = Emitter(settings, writer)
        val serializer = Serializer(settings, emitter)
        
        serializer.emitStreamStart()
        serializer.serializeDocument(node)
        serializer.emitStreamEnd()
    }
    
}