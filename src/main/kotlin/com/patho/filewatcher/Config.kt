package com.patho.filewatcher

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service

@Service
@ConfigurationProperties(
        prefix = "watcher"
)
class Config {
    lateinit var dirToWatch: String

    lateinit var splitImages: List<SubImageBorders>

    var appendToPrevPageDetectionThreshold: Int = 0

    var moveProcessedFiles = false
    lateinit var processedFilesDir: String

    var moveOriginalFiles = false
    lateinit var originalFilesDir: String

    var uploadData = true
    lateinit var uploadTarget: String

    lateinit var errorAddresses : List<String>

    class SubImageBorders {
        var x: Int = 0
        var y: Int = 0
        var width: Int = 0
        var height: Int = 0
        lateinit var jsonTag: String
    }


}