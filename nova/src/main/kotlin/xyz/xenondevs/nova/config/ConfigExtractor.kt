package xyz.xenondevs.nova.config

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
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.nova.util.data.NodeWalkDecision
import xyz.xenondevs.nova.util.data.deepEquals
import xyz.xenondevs.nova.util.data.get
import xyz.xenondevs.nova.util.data.remove
import xyz.xenondevs.nova.util.data.set
import xyz.xenondevs.nova.util.data.walk
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

internal class ConfigExtractor(extractedConfigs: MutableProvider<Map<String, String>>) {
    
    private var extractedConfigs: Map<String, String> by extractedConfigs
    
    fun extract(configPath: String, fileInZip: Path, destFile: Path) {
        val internalCfg = loadYaml(fileInZip)
        val extractedCfg = extractedConfigs[configPath]?.let(::loadYaml)
        
        val severCfg: Node
        if (!destFile.exists() || extractedCfg == null) {
            severCfg = internalCfg
            extractedConfigs = extractedConfigs.toMutableMap().apply { put(configPath, writeYaml(severCfg, false)) }
        } else {
            severCfg = loadYaml(destFile)
            updateExistingConfig(severCfg, extractedCfg, internalCfg)
            extractedConfigs = extractedConfigs.toMutableMap().apply { put(configPath, writeYaml(extractedCfg, false)) }
        }
        
        writeYaml(severCfg, destFile)
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
    
    private fun loadYaml(s: String): Node {
        return loadYaml(s.byteInputStream())
    }
    
    private fun loadYaml(file: Path): Node {
        return file.inputStream().use { inp -> loadYaml(inp) }
    }
    
    private fun loadYaml(inp: InputStream): Node {
        val settings = LoadSettings.builder()
            .setParseComments(true)
            .build()
        val reader = StreamReader(settings, inp.reader())
        val parser = ParserImpl(settings, reader)
        val composer = Composer(settings, parser)
        return composer.singleNode.get()
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