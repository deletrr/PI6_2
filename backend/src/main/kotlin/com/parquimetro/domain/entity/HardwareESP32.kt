package com.parquimetro.domain.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "hardware_esp32")
class HardwareESP32(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @OneToOne
    @JoinColumn(name = "vaga_id", nullable = false, unique = true)
    val vaga: Vaga,

    @Column(nullable = false, unique = true)
    val deviceId: String,

    @Column(nullable = false)
    val hmacSecret: String,

    @Column(nullable = false)
    var firmwareVersion: String,

    @Column(nullable = false)
    var batteryLevel: Int,

    @Column(nullable = false)
    var lastSeen: Instant = Instant.now(),

    @Column(nullable = false)
    var ativo: Boolean = true
)
