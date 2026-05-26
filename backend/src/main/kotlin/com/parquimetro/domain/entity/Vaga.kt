package com.parquimetro.domain.entity

import jakarta.persistence.*
import org.locationtech.jts.geom.Point
import java.util.UUID

@Entity
@Table(name = "vagas")
class Vaga(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    val codigo: String,

    @Column(columnDefinition = "geometry(Point,4326)", nullable = false)
    val localizacao: Point,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: VagaStatus = VagaStatus.LIVRE,

    @OneToOne(mappedBy = "vaga", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var hardware: HardwareESP32? = null
)
