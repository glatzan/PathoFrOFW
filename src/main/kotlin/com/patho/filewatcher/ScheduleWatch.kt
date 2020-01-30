package com.patho.filewatcher

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat
import java.util.*

@Service
@ConditionalOnProperty(name = ["watcher.schedule.enable"], havingValue = "true")
open class ScheduleWatch @Autowired constructor(
        private val mainService: MainService) {

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @Scheduled(cron = "\${watcher.schedule.cron}")
    fun reportCurrentTime() {
        val dateFormat = SimpleDateFormat("HH:mm:ss");
        logger.info("The time is now {}", dateFormat.format(Date()))
        logger.info("Starting scheduled watch of dir")
        mainService.watchDir()
    }
}