package com.patho.filewatcher

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MainService @Autowired constructor(
        private val watcher: WatcherService,
        private val mailService: MailService,
        private val config: Config,
        private val restService: RestService) {

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    public fun watchDir() {
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

            results.forEach {
                if (it.valid) {
                    val result = restService.uploadFile(it, config.uploadTarget, config.useAuthentication, config.authenticationToken)
                    if (!result) {
                        mailService.sendMail(config.errorAddresses.first(), "Fehler beim hochladen des PDFs", "", it)
                    }
                } else {
                    logger.debug("Not valid pdf sending mail")
                    mailService.sendMail(config.errorAddresses.first(), "Fehler PDF nicht zugeordnet", "", it)
                }
            }

        } else
            logger.error("No file found! End")

        logger.debug("----------------------------")
        logger.debug("---------- End -------------")
        logger.debug("----------------------------")
    }
}