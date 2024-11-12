package xyz.xenondevs.nova.util.data.http

import io.ktor.http.*
import io.ktor.http.ContentType.Application.OctetStream
import io.ktor.http.content.OutgoingContent.*
import io.ktor.utils.io.*
import java.io.InputStream

internal class BinaryBufferedBody(val stream: InputStream, override val contentType: ContentType = OctetStream) : WriteChannelContent() {
    
    override suspend fun writeTo(channel: ByteWriteChannel) {
        val buffer = ByteArray(8192)
        var len = 0
        while ({ len = stream.read(buffer); len }() != 0) {
            channel.writeFully(buffer, 0, len)
        }
    }
    
}

