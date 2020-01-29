package com.patho.filewatcher

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate

@Service
class RestService {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    fun uploadFile(pdf :PDFPageResult, targetURl : String){
        val bodyMap: MultiValueMap<String, Any> = LinkedMultiValueMap()
        bodyMap.add("file", pdf.pdfAsByts)
        bodyMap.add("info", pdf.getJson())

        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA
        val requestEntity: HttpEntity<MultiValueMap<String, Any>> = HttpEntity(bodyMap, headers)

        logger.debug("Sending pdf (${pdf.target}) to $targetURl")

        val restTemplate = RestTemplate()
        val response: ResponseEntity<String> = restTemplate.exchange(targetURl,
                HttpMethod.POST, requestEntity, String::class.java)

        logger.debug("response status: " + response.statusCode)
        logger.debug("response body: " + response.body)
    }
}