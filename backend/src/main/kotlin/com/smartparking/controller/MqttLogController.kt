package com.smartparking.controller

import com.smartparking.dto.MqttLogResponse
import com.smartparking.dto.toResponse
import com.smartparking.repository.MqttLogRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/mqtt-logs")
@PreAuthorize("hasRole('ADMIN')")
class MqttLogController(private val mqttLogRepository: MqttLogRepository) {

    @GetMapping
    fun getLogs(): ResponseEntity<List<MqttLogResponse>> =
        ResponseEntity.ok(mqttLogRepository.findTop100ByOrderByCreatedAtDesc().map { it.toResponse() })
}
