package com.patho.filewatcher.service

import com.patho.filewatcher.model.PDFPageResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service


@Service
class MailService @Autowired constructor(
        private val emailSender: JavaMailSender) {

    fun sendMail(to: String, subject: String, text: String) {
        val message = SimpleMailMessage()
        message.setTo(to)
        message.setSubject(subject)
        message.setText(text)
        emailSender.send(message)
    }

    fun sendMail(to: String, subject: String, text: String, pdf : PDFPageResult) {
        val message = emailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true)

        helper.setTo(to)
        helper.setSubject(subject)
        helper.setText(text + " " +pdf.getJson())
        helper.addAttachment("error.pdf", ByteArrayResource(pdf.pdfAsByts),"application/pdf")

        emailSender.send(message)
    }
}