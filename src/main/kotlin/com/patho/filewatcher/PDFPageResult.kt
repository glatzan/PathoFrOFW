package com.patho.filewatcher

import org.apache.pdfbox.pdmodel.PDDocument
import org.json.JSONObject

class PDFPageResult {

    var pages: MutableList<PDDocument> = mutableListOf()
    var tags: HashMap<String, String> = hashMapOf()

    var target: String = ""

    var pdfAsByts = byteArrayOf()

    fun getJson(): String {
        val jo = JSONObject(tags)
        return jo.toString()
    }
}