package com.patho.filewatcher

import org.springframework.core.io.ByteArrayResource

open class MultiPartResource(bytes: ByteArray, private val fileN : String) : ByteArrayResource(bytes){
    override fun getFilename(): String? {
        return fileN
    }
}