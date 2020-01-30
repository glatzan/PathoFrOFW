package com.patho.filewatcher

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate

@Service
class RestService {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    fun uploadFile(pdf: PDFPageResult, targetURl: String, useAuthentication: Boolean = false, token: String = "") : Boolean{
        val bodyMap: MultiValueMap<String, Any> = LinkedMultiValueMap()
        bodyMap.add("piz", pdf.getPiz().second)
        bodyMap.add("caseID", pdf.getCaseID().second)
        bodyMap.add("documentType", pdf.getDocumentID().second)
        bodyMap.add("user-file", MultiPartResource(pdf.pdfAsByts, "dummy.pdf"))

        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA

        if (useAuthentication)
            headers.set("Authorization", token);

        val requestEntity: HttpEntity<MultiValueMap<String, Any>> = HttpEntity(bodyMap, headers)

        logger.debug("Sending pdf (${pdf.target}) to $targetURl")

        val restTemplate = RestTemplate()
        val response: ResponseEntity<String> = restTemplate.exchange(targetURl,
                HttpMethod.POST, requestEntity, String::class.java)

        logger.debug("response status: " + response.statusCode)
        logger.debug("response body: " + response.body)
        return response.statusCode.toString() == "200 OK"
    }
}