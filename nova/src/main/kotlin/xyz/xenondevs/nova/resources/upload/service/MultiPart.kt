package xyz.xenondevs.nova.resources.upload.service

// TODO

//internal abstract class MultiPart : UploadService {
//    
//    protected abstract val url: String
//    protected abstract val filePartName: String
//    protected abstract val urlRegex: Regex?
//    protected abstract val extraParams: Map<String, String>
//    
//    override suspend fun upload(file: Path): String {
//        val response = HTTP_CLIENT.submitFormWithBinaryData(url, formData {
//            append(filePartName, file.readBytes(), Headers.build {
//                append(HttpHeaders.ContentType, ContentType.Application.Zip)
//                append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
//            })
//            extraParams.forEach(::append)
//        })
//        val responseString = response.body<String>()
//        require(response.status == HttpStatusCode.OK) { "Upload failed with status ${response.status} amd response $responseString" }
//        return if (urlRegex == null) responseString else urlRegex!!.find(responseString)?.groupValues?.getOrNull(1)
//            ?: throw IllegalArgumentException("No url found in response: $responseString")
//    }
//    
//    override fun disable() = Unit
//    
//}
//
//internal object CustomMultiPart : MultiPart() {
//    
//    override val names = setOf("custom_multi_part", "custommultipart")
//    
//    override lateinit var url: String
//    override lateinit var filePartName: String
//    public override var urlRegex: Regex? = null
//    override val extraParams = HashMap<String, String>()
//    
//    override fun loadConfig(cfg: ConfigurationNode) {
//        url = cfg.node("url").string
//            ?: throw IllegalArgumentException("No url specified for CustomMultiPart")
//        filePartName = cfg.node("filePartName").string
//            ?: throw IllegalArgumentException("No filePartName specified for CustomMultiPart")
//        urlRegex = cfg.node("urlRegex")?.get<Regex>()
//        cfg.node("extraParams").childrenMap().forEach { (key, value) ->
//            this.extraParams[key as String] = value.toString()
//        }
//    }
//    
//}