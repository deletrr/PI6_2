package com.parquimetro

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class ParquimetroApplication

fun main(args: Array<String>) {
    runApplication<ParquimetroApplication>(*args)
}
