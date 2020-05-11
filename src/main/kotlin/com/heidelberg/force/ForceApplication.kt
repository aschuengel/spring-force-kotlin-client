package com.heidelberg.force

import com.heidelberg.force.service.ForceService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import kotlin.system.exitProcess

@SpringBootApplication
class ForceApplication(val forceService: ForceService) : CommandLineRunner {
    private val logger: Logger = LoggerFactory.getLogger(ForceApplication::class.java)

    override fun run(vararg args: String?) {
        val username = System.getenv("SFDC_USERNAME")
        val password = System.getenv("SFDC_PASSWORD")
        val securityToken = System.getenv("SFDC_SECURITY_TOKEN")
        val (sessionId, serverUrl) = forceService.login(username, password, securityToken)
        logger.info("""Session id: $sessionId""")
        logger.info("""Server URL: $serverUrl""")
        val versions = forceService.getVersions()
        versions?.forEach {
            logger.info("Version: ${it.label}, ${it.version}, ${it.url}")
        }
        val limits = forceService.getLimits()
        limits?.forEach {
            logger.info("Limit ${it.key}: max=${it.value.max}, remaining=${it.value.remaining}")
        }
        val services = forceService.getServices()
        services?.forEach {
            logger.info("Service ${it.key}: ${it.value}")
        }
        val sobjects = forceService.getSObjects()
        sobjects?.sobjects?.forEach {
            logger.info("SObject ${it.name} ${it.label}")
        }
        exitProcess(0)
    }
}

fun main(args: Array<String>) {
    runApplication<ForceApplication>(*args)
}
