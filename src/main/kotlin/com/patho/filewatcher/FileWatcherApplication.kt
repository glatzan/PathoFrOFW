package com.patho.filewatcher

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Lazy
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.util.*


@SpringBootApplication
@EnableConfigurationProperties
class FileWatcherApplication @Autowired constructor(
        private val watcher: WatcherService,
        private val mailService: MailService,
        private val mailSender: JavaMailSender,
        private val config: Config,
        private val restService: RestService) : CommandLineRunner {

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    override fun run(vararg args: String?) {
        logger.debug("----------------------------")
        logger.debug("--------- Start ------------")
        logger.debug("----------------------------")

        logger.debug("appendToPrevPageDetectionThreshold = ${config.appendToPrevPageDetectionThreshold}")
        logger.debug("Getting files of dir ${config.dirToWatch}")


        val files = watcher.getFilesOFWatchDir(config.dirToWatch)

        if (files.isNotEmpty()) {
            files.forEach { logger.debug("Found file -> ${it.filename}") }

            logger.debug("Reading PDFs")
            val results = watcher.processPDFs(files.map { it.file }, config.moveProcessedFiles,
                    config.processedFilesDir, config.moveOriginalFiles, config.originalFilesDir)

            val json = results.map { it.getJson() }
            json.forEach { println(it) }

            results.forEach {
                restService.uploadFile(it, config.uploadTarget)
            }

        } else
            logger.error("No file found! End program")


    }


}

fun main(args: Array<String>) {
    runApplication<FileWatcherApplication>(*args)
}

