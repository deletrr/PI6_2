package com.smartparking.service

import com.smartparking.dto.*
import com.smartparking.entity.ParkingMeter
import com.smartparking.entity.ParkingStatus
import com.smartparking.repository.ParkingMeterRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class ParkingMeterService(
    private val meterRepository: ParkingMeterRepository,
    private val messagingTemplate: SimpMessagingTemplate
) {

    fun listAll(): List<ParkingMeterResponse> =
        meterRepository.findAll().map { it.toResponse() }

    fun listForMap(): List<ParkingMeterResponse> =
        meterRepository.findAllActiveWithCoordinates().map { it.toResponse() }

    fun listOrphans(): List<ParkingMeterResponse> =
        meterRepository.findByOrphan(true).map { it.toResponse() }

    fun getById(id: UUID): ParkingMeterResponse {
        val meter = meterRepository.findById(id)
            .orElseThrow { NoSuchElementException("Parquímetro não encontrado.") }
        return meter.toResponse()
    }

    fun getByCode(code: String): ParkingMeterResponse {
        val meter = meterRepository.findByCode(code)
            .orElseThrow { NoSuchElementException("Parquímetro não encontrado: $code") }
        return meter.toResponse()
    }

    fun search(search: String?, page: Int, size: Int): PageResponse<ParkingMeterResponse> {
        val pageable = PageRequest.of(page, size, Sort.by("code").ascending())
        val searchPattern = search?.trim()?.let { if (it.isEmpty()) null else "%${it.lowercase()}%" }
        val result = meterRepository.searchMeters(searchPattern, pageable)
        return PageResponse(
            content = result.content.map { it.toResponse() },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            page = page,
            size = size
        )
    }

    @Transactional
    fun create(request: CreateParkingMeterRequest): ParkingMeterResponse {
        if (meterRepository.existsByCode(request.code)) {
            throw IllegalArgumentException("Código já em uso: ${request.code}")
        }
        val meter = ParkingMeter(
            code = request.code.uppercase().trim(),
            description = request.description?.trim(),
            latitude = request.latitude,
            longitude = request.longitude,
            mqttTopic = "parquimetro/${request.code.uppercase().trim()}/status",
            orphan = request.latitude == null || request.longitude == null
        )
        return meterRepository.save(meter).toResponse()
    }

    @Transactional
    fun update(id: UUID, request: UpdateParkingMeterRequest): ParkingMeterResponse {
        val meter = meterRepository.findById(id)
            .orElseThrow { NoSuchElementException("Parquímetro não encontrado.") }
        request.description?.let { meter.description = it.trim() }
        request.latitude?.let { meter.latitude = it }
        request.longitude?.let { meter.longitude = it }
        request.active?.let { meter.active = it }
        request.orphan?.let { meter.orphan = it }

        // Se coordenadas foram definidas, remover flag de órfão automaticamente
        if (request.latitude != null && request.longitude != null) {
            meter.orphan = false
        }

        return meterRepository.save(meter).toResponse()
    }

    @Transactional
    fun delete(id: UUID) {
        val meter = meterRepository.findById(id)
            .orElseThrow { NoSuchElementException("Parquímetro não encontrado.") }
        
        // Se houver sessões vinculadas, apenas desativa. Caso contrário, deleta.
        // Aqui assumimos que se o usuário quer "excluir", ele quer sumir com o registro.
        try {
            meterRepository.delete(meter)
        } catch (e: Exception) {
            meter.active = false
            meterRepository.save(meter)
        }
    }

    @Transactional
    fun updateStatus(code: String, newStatus: ParkingStatus): ParkingMeter {
        val meter = meterRepository.findByCode(code).orElseGet {
            ParkingMeter(
                code = code.uppercase().trim(),
                mqttTopic = "parquimetro/${code.uppercase().trim()}/status",
                orphan = true,
                status = newStatus
            )
        }
        meter.status = newStatus
        meter.lastSeen = LocalDateTime.now()
        val saved = meterRepository.save(meter)
        messagingTemplate.convertAndSend("/topic/meters", saved.toResponse())
        return saved
    }
}
