package xyz.xenondevs.nova.resources.upload.service

// TODO

//internal object S3 : UploadService {
//    
//    override val names = setOf("s3", "amazon_s3")
//    
//    private var client: S3Client? = null
//    private lateinit var bucket: String
//    private lateinit var directory: String
//    private lateinit var urlFormat: String
//    
//    override fun loadConfig(cfg: ConfigurationNode) {
//        val endpoint = cfg.node("endpoint").string?.removePrefix("https://")?.removePrefix("http://")
//            ?: throw IllegalArgumentException("S3 endpoint is not specified")
//        val endpointURI = URI("https://$endpoint")
//        val keyId = cfg.node("key_id").string
//            ?: throw IllegalArgumentException("S3 key_id is not specified")
//        val keySecret = cfg.node("key_secret").string
//            ?: throw IllegalArgumentException("S3 key_secret is not specified")
//        val credentials = AwsBasicCredentials.create(keyId, keySecret)
//        
//        val region = cfg.node("region")?.string?.let(Region::of)
//            ?: throw IllegalArgumentException("S3 region is not specified. Regions available by default are: " +
//                Region.regions().joinToString(", ", transform = Region::id))
//        
//        val forcePathStyle = cfg.node("force_path_style").getBoolean(false)
//        
//        LOGGER.info("Connecting to S3 endpoint $endpoint")
//        this.client = S3Client.builder()
//            .endpointOverride(endpointURI)
//            .credentialsProvider(StaticCredentialsProvider.create(credentials))
//            .region(region)
//            .forcePathStyle(forcePathStyle)
//            .build()
//        this.bucket = cfg.node("bucket").string
//            ?: throw IllegalArgumentException("S3 bucket is not specified")
//        if (this.client!!.listBuckets().buckets().none { it.name() == bucket })
//            throw IllegalArgumentException("S3 bucket $bucket not found")
//        
//        this.directory = (cfg.node("directory").string?.addSuffix("/") ?: "")
//        urlFormat = "https://$endpoint/$bucket/$directory%s"
//    }
//    
//    override suspend fun upload(file: Path): String {
//        val name = StringUtils.randomString(5)
//        val req = PutObjectRequest.builder()
//            .bucket(bucket)
//            .key(directory + name)
//            .build()
//        val resp = client!!.putObject(req, file).sdkHttpResponse()
//        
//        if (!resp.isSuccessful)
//            throw IllegalStateException("S3 upload failed with code ${resp.statusCode()} " + resp.statusText().orElse(""))
//        
//        val lastUpload: String? = PermanentStorage.retrieve("lastS3Upload")
//        if (lastUpload != null && lastUpload.startsWith(urlFormat.dropLast(2 + directory.length))) {
//            val lastBucket = lastUpload.drop("https://".length).split("/")[1]
//            val delReq = DeleteObjectRequest.builder()
//                .bucket(lastBucket)
//                .key(lastUpload.split('/', limit = 5)[4])
//                .build()
//            
//            val delResp = client!!.deleteObject(delReq)
//            if (!delResp.sdkHttpResponse().isSuccessful)
//                LOGGER.warn("S3 delete of old resourcepack failed with code ${delResp.sdkHttpResponse().statusCode()} "
//                    + delResp.sdkHttpResponse().statusText().orElse(""))
//        }
//        
//        val url = urlFormat.format(name)
//        PermanentStorage.store("lastS3Upload", url)
//        return url
//    }
//    
//    override fun disable() {
//        client = null
//    }
//}