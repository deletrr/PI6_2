package com.parquimetro.domain.entity

import jakarta.persistence.*
import org.locationtech.jts.geom.Point
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "infracoes")
class Infracao(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaga_id", nullable = false)
    val vaga: Vaga,

    @Column(nullable = false)
    val fiscalId: String,

    @Column(nullable = false)
    val fotoHash: String,

    @Column(columnDefinition = "geometry(Point,4326)", nullable = false)
    val localizacao: Point,

    @Column(nullable = false)
    val registradaEm: Instant = Instant.now()
)

@Entity
@Table(name = "pagamentos")
class Pagamento(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaga_id", nullable = false)
    val vaga: Vaga,

    @Column(nullable = false)
    val motoristaCpf: String,

    @Column(nullable = false)
    val placa: String,

    @Column(nullable = false)
    val criadoEm: Instant = Instant.now(),

    @Column(nullable = false)
    val expiraEm: Instant,

    @Column(nullable = false)
    val valor: Double,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var statusPagamento: StatusPagamento = StatusPagamento.PENDENTE
)

enum class StatusPagamento { PENDENTE, PAGO, EXPIRADO }
