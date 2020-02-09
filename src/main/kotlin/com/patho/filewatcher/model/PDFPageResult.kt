package com.patho.filewatcher.model

import com.google.gson.Gson
import org.apache.pdfbox.pdmodel.PDDocument
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PDFPageResult {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    var pages: MutableList<PDDocument> = mutableListOf()
    var tags: HashMap<String, String> = hashMapOf()

    var target: String = ""

    var pdfAsByts = byteArrayOf()

    var valid : Boolean = false

    fun getPiz(): Pair<Int, String> {
        val piz = tags["piz"]
        val qrPiz = tags["qr"]?.substringBefore(";")

        return getStringAnCertainty(Regex("[0-9]{8,}"), piz, qrPiz)
    }

    fun getCaseID(): Pair<Int, String> {
        val caseID = tags["caseNumber"]
        val arr = tags["qr"]?.split(";") ?: listOf()
        val qrCaseID = if (arr.size == 3) arr[1] else ""

        return getStringAnCertainty(Regex("[0-9]{6,}"), caseID, qrCaseID)
    }

    fun getDocumentID(): Pair<Int, String> {
        val documentID = tags["documentNumber"]
        val qrDocumentID = tags["qr"]?.substringAfterLast(";")

        return getStringAnCertainty(Regex("[0-9]{8,}"), documentID, qrDocumentID)
    }

    /**
     * Checks if str1 matches str2, if not str2 (qr) is preferred.
     */
    fun getStringAnCertainty(reg: Regex, str1: String?, str2: String?): Pair<Int, String> {
        var str1OK = false
        var str2OK = false

        if (str1 != null && str1.matches(reg)) {
            str1OK = true
        }

        if (str2 != null && str2.matches(reg)) {
            str2OK = true
        }

        return if (str1OK && str2OK && str1 == str2) {
            logger.debug("High certainty = $str1")
            Pair(3, str1 ?: "")
        } else {
            // prefer qr
            if (str2OK) {
                logger.debug("QR certainty = $str2")
                Pair(2, str2 ?: "")
            } else if (str1OK) {
                logger.debug("Bar certainty = $str1")
                Pair(1, str1 ?: "")
            } else {
                logger.debug("Not found!")
                Pair(0, "")
            }
        }
    }

    fun getJson(): String {
        val jo = Gson().toJson(tags)
        return jo.toString()
    }
}