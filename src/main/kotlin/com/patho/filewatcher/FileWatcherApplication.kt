package com.patho.filewatcher

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.text.SimpleDateFormat
import java.util.*


@SpringBootApplication
@EnableConfigurationProperties
@EnableScheduling
class FileWatcherApplication @Autowired constructor(
        private val config: Config,
        private val mainService: MainService) : CommandLineRunner {

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    override fun run(vararg args: String?) {
        if(config.schedule.enable)
            logger.debug("Started with cron mode...")
        else{
            logger.debug("Started in normal mode...")
            mainService.watchDir()
        }
    }

}

fun main(args: Array<String>) {
    runApplication<FileWatcherApplication>(*args)
}

