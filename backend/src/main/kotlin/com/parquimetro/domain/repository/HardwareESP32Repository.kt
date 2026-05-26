package com.parquimetro.domain.repository

import com.parquimetro.domain.entity.HardwareESP32
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface HardwareESP32Repository : JpaRepository<HardwareESP32, UUID> {
    fun findByDeviceId(deviceId: String): HardwareESP32?
}
