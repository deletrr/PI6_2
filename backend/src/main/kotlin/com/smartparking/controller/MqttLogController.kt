package com.smartparking.controller

import com.smartparking.entity.MqttLog
import com.smartparking.repository.MqttLogRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/mqtt-logs")
@PreAuthorize("hasRole('ADMIN')")
class MqttLogController(private val mqttLogRepository: MqttLogRepository) {

    @GetMapping
    fun getLogs(): ResponseEntity<List<MqttLog>> =
        ResponseEntity.ok(mqttLogRepository.findTop100ByOrderByCreatedAtDesc())
}
