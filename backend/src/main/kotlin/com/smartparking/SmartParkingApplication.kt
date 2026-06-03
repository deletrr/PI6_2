package com.smartparking

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class PontoLivreApplication

fun main(args: Array<String>) {
    runApplication<PontoLivreApplication>(*args)
}
