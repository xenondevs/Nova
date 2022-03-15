package xyz.xenondevs.nova.util.data

import com.google.gson.JsonParser
import xyz.xenondevs.nova.util.StringUtils
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.nio.charset.Charset

class HttpMultipartRequest(
    requestUrl: String,
    requestMethod: String,
    private val charset: Charset = Charsets.UTF_8,
    connectionTransform: HttpURLConnection.() -> Unit = {}
) {
    
    private val connection = URL(requestUrl).openConnection() as HttpURLConnection
    private val boundary = StringUtils.randomString(25)
    private val outputStream: OutputStream
    private val writer: PrintWriter
    
    init {
        connection.useCaches = true
        connection.doOutput = true
        connection.doInput = true
        connection.requestMethod = requestMethod
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        
        connection.connectionTransform()
        
        outputStream = connection.outputStream
        writer = PrintWriter(OutputStreamWriter(outputStream, charset))
    }
    
    fun addHeaderField(name: String, value: String) {
        writer.printlnAndFlush("$name: $value")
    }
    
    fun addFormField(name: String, value: String) {
        writer.println("--$boundary")
        writer.println("Content-Disposition: form-data; name=$name")
        writer.println("Content-Type: text/plain; charset=$charset")
        writer.println()
        writer.printlnAndFlush(value)
    }
    
    fun addFormFile(fieldName: String = "file", file: File, fileName: String = file.name) {
        writer.println("--$boundary")
        writer.println("Content-Disposition: form-data; name=$fieldName; filename=$fileName")
        writer.println("Content-Type: " + URLConnection.guessContentTypeFromName(fileName))
        writer.println("Content-Transfer-Encoding: binary")
        writer.printlnAndFlush()
        
        val inputStream = file.inputStream()
        inputStream.copyTo(outputStream)
        outputStream.flush()
        inputStream.close()
        
        writer.printlnAndFlush()
    }
    
    fun complete(): HttpResponse {
        writer.flush()
        writer.println("--$boundary--")
        writer.close()
        
        val responseCode = connection.responseCode
        val inputStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        return HttpResponse(
            responseCode,
            inputStream.bufferedReader(charset).use(BufferedReader::readText)
        )
    }
    
    private fun PrintWriter.printlnAndFlush() {
        println()
        flush()
    }
    
    private fun PrintWriter.printlnAndFlush(string: String) {
        println(string)
        flush()
    }
    
    class HttpResponse(val statusCode: Int, val body: String) {
        val isSuccessful: Boolean
            get() = statusCode in 200..299
        val jsonResponse
            get() = JsonParser.parseString(body)
                ?: throw IllegalStateException("Response body is not a valid JSON string")
    }
    
}