package xyz.xenondevs.nova.data.resources.upload

import xyz.xenondevs.nova.data.resources.upload.service.Xenondevs
import java.io.File

object UploadManager {
    
    private val SERVICES: List<UploadService> = listOf(Xenondevs)
    
    fun uploadPack(service: String, pack: File): String {
        val uploadService = SERVICES.firstOrNull { it.name == service }
        requireNotNull(uploadService) { "Uploader $service not found" }
        check(pack.exists()) { "ResourcePack.zip not found!" }
        return uploadService.upload(pack)
    }
    
}