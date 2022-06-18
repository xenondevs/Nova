package xyz.xenondevs.nova.util.data.http

import io.ktor.http.*
import io.ktor.http.ContentType.Application.OctetStream
import io.ktor.http.content.OutgoingContent.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import java.io.InputStream

internal class BinaryBufferedBody(val stream: InputStream, override val contentType: ContentType = OctetStream) : WriteChannelContent() {
    
    override suspend fun writeTo(channel: ByteWriteChannel) {
        stream.copyTo(channel)
    }
    
}

