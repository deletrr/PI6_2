package com.parquimetro.service

import com.parquimetro.api.dto.VagaCreateRequest
import com.parquimetro.api.dto.VagaResponse
import com.parquimetro.domain.entity.Vaga
import com.parquimetro.domain.entity.VagaStatus
import com.parquimetro.domain.repository.VagaRepository
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class VagaService(private val vagaRepo: VagaRepository) {

    private val gf = GeometryFactory(PrecisionModel(), 4326)

    fun findAll(pageable: Pageable): Page<VagaResponse> =
        vagaRepo.findAll(pageable).map { it.toResponse() }

    fun findById(id: UUID): VagaResponse =
        vagaRepo.findById(id).orElseThrow().toResponse()

    @Transactional
    fun create(req: VagaCreateRequest): VagaResponse {
        val point = gf.createPoint(Coordinate(req.lng, req.lat))
        return vagaRepo.save(Vaga(codigo = req.codigo, localizacao = point)).toResponse()
    }

    @Transactional
    fun updateStatus(id: UUID, status: VagaStatus): VagaResponse {
        val vaga = vagaRepo.findById(id).orElseThrow()
        vaga.status = status
        return vagaRepo.save(vaga).toResponse()
    }

    private fun Vaga.toResponse() = VagaResponse(
        id = id,
        codigo = codigo,
        lat = localizacao.y,
        lng = localizacao.x,
        status = status,
        battery = hardware?.batteryLevel
    )
}
