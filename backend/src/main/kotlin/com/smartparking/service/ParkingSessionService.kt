package com.smartparking.service

import com.smartparking.config.ParkingRulesConfig
import com.smartparking.dto.*
import com.smartparking.entity.*
import com.smartparking.repository.*
import com.smartparking.security.CustomUserDetailsService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class ParkingSessionService(
    private val sessionRepository: ParkingSessionRepository,
    private val meterRepository: ParkingMeterRepository,
    private val userRepository: UserRepository,
    private val walletTransactionRepository: WalletTransactionRepository,
    private val fineRepository: FineRepository,
    private val billingService: BillingService,
    private val userDetailsService: CustomUserDetailsService,
    private val rules: ParkingRulesConfig,
    private val messagingTemplate: SimpMessagingTemplate
) {

    @Transactional
    fun startSession(email: String, request: StartSessionRequest): SessionResponse {
        val user = userDetailsService.loadUserEntityByEmail(email)

        // Verificar sessão ativa existente
        sessionRepository.findByUserIdAndStatus(user.id, SessionStatus.ACTIVE).ifPresent {
            throw IllegalStateException("Você já possui uma sessão ativa no parquímetro ${it.parkingMeter.code}.")
        }

        val meter = meterRepository.findByCode(request.meterCode)
            .orElseThrow { NoSuchElementException("Parquímetro não encontrado: ${request.meterCode}") }

        if (!meter.active) throw IllegalStateException("Parquímetro inativo.")
        if (meter.status == ParkingStatus.OCCUPIED) {
            throw IllegalStateException("Vaga já ocupada.")
        }

        val now = LocalDateTime.now()
        val session = ParkingSession(
            user = user,
            parkingMeter = meter,
            vehiclePlate = request.vehiclePlate,
            startTime = now,
            freeUntil = now.plusMinutes(rules.freeToleranceMinutes)
        )

        meter.status = ParkingStatus.OCCUPIED
        meterRepository.save(meter)
        sessionRepository.save(session)

        messagingTemplate.convertAndSend("/topic/meters", meter.toResponse())
        messagingTemplate.convertAndSend("/topic/sessions/${user.id}", session.toResponse())

        return session.toResponse(elapsedMinutes = 0, estimatedCost = BigDecimal.ZERO)
    }

    @Transactional
    fun endSession(email: String, sessionId: UUID): SessionResponse {
        val user = userDetailsService.loadUserEntityByEmail(email)
        val session = sessionRepository.findByIdWithDetails(sessionId)
            .orElseThrow { NoSuchElementException("Sessão não encontrada.") }

        if (session.user.id != user.id && user.role != UserRole.ADMIN) {
            throw IllegalAccessException("Sessão não pertence a este usuário.")
        }

        return closeSession(session)
    }

    @Transactional
    fun endSessionByMqtt(meterId: UUID): SessionResponse? {
        val session = sessionRepository.findByParkingMeterIdAndStatus(meterId, SessionStatus.ACTIVE)
            .orElse(null) ?: return null
        return closeSession(session)
    }

    private fun closeSession(session: ParkingSession): SessionResponse {
        if (session.status != SessionStatus.ACTIVE) {
            throw IllegalStateException("Sessão já encerrada.")
        }

        val endTime = LocalDateTime.now()
        val billing = billingService.calculateCharge(session.startTime, endTime)
        val isOvertime = billingService.isOvertimeExceeded(session.startTime)

        session.endTime = endTime
        session.amountCharged = billing.amountCharged
        session.chargedHours = billing.chargedHours
        session.overtime = isOvertime
        session.status = if (isOvertime) SessionStatus.OVERTIME else SessionStatus.CLOSED

        val meter = session.parkingMeter
        meter.status = ParkingStatus.FREE
        meterRepository.save(meter)

        // RN05 — debitar saldo
        if (billing.amountCharged > BigDecimal.ZERO) {
            debitWallet(session, billing.amountCharged, "Cobrança de estacionamento - ${meter.code}")
        }

        // RN06 — aplicar multa se overtime
        if (isOvertime) {
            applyOvertimeFine(session)
        }

        sessionRepository.save(session)

        messagingTemplate.convertAndSend("/topic/meters", meter.toResponse())
        messagingTemplate.convertAndSend(
            "/topic/sessions/${session.user.id}",
            session.toResponse()
        )

        return session.toResponse()
    }

    private fun debitWallet(session: ParkingSession, amount: BigDecimal, description: String) {
        val user = session.user
        val balanceBefore = user.balance
        val balanceAfter = (balanceBefore - amount).coerceAtLeast(BigDecimal.ZERO)
        user.balance = balanceAfter
        userRepository.save(user)

        val tx = WalletTransaction(
            user = user,
            session = session,
            type = TransactionType.DEBIT_SESSION,
            amount = amount,
            balanceBefore = balanceBefore,
            balanceAfter = balanceAfter,
            description = description
        )
        walletTransactionRepository.save(tx)
    }

    private fun applyOvertimeFine(session: ParkingSession) {
        val fine = Fine(
            user = session.user,
            session = session,
            amount = rules.overtimeFine,
            reason = "Tempo máximo de permanência excedido (${rules.maxSessionHours}h). " +
                     "Sessão encerrada automaticamente."
        )
        fineRepository.save(fine)

        val user = session.user
        val balanceBefore = user.balance
        val balanceAfter = (balanceBefore - rules.overtimeFine).coerceAtLeast(BigDecimal.ZERO)
        user.balance = balanceAfter
        userRepository.save(user)

        val tx = WalletTransaction(
            user = user,
            session = session,
            type = TransactionType.DEBIT_FINE,
            amount = rules.overtimeFine,
            balanceBefore = balanceBefore,
            balanceAfter = balanceAfter,
            description = "Multa por tempo excedido - ${session.parkingMeter.code}"
        )
        walletTransactionRepository.save(tx)
    }

    fun getActiveSession(email: String): SessionResponse? {
        val user = userDetailsService.loadUserEntityByEmail(email)
        val session = sessionRepository.findByUserIdAndStatus(user.id, SessionStatus.ACTIVE)
            .orElse(null) ?: return null

        val elapsedMinutes = ChronoUnit.MINUTES.between(session.startTime, LocalDateTime.now())
        val estimatedCost = billingService.estimateCurrentCost(session.startTime)

        return session.toResponse(elapsedMinutes = elapsedMinutes, estimatedCost = estimatedCost)
    }

    fun getSessionById(id: UUID): SessionResponse {
        val session = sessionRepository.findByIdWithDetails(id)
            .orElseThrow { NoSuchElementException("Sessão não encontrada.") }
        return session.toResponse()
    }

    fun getUserSessions(email: String, page: Int, size: Int): PageResponse<SessionResponse> {
        val user = userDetailsService.loadUserEntityByEmail(email)
        val pageable = PageRequest.of(page, size, Sort.by("startTime").descending())
        val result = sessionRepository.findByUserId(user.id, pageable)
        return PageResponse(
            content = result.content.map { it.toResponse() },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            page = page,
            size = size
        )
    }

    fun getAllSessions(page: Int, size: Int): PageResponse<SessionResponse> {
        val pageable = PageRequest.of(page, size, Sort.by("startTime").descending())
        val result = sessionRepository.findAll(pageable)
        return PageResponse(
            content = result.content.map { it.toResponse() },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            page = page,
            size = size
        )
    }

    // RN06 — job para encerrar sessões que ultrapassaram 2h
    @Transactional
    fun enforceMaxSessionTime() {
        val cutoff = LocalDateTime.now().minusHours(rules.maxSessionHours)
        val overtimeSessions = sessionRepository.findActiveSessionsOlderThan(cutoff)
        overtimeSessions.forEach { session ->
            runCatching { closeSession(session) }
        }
    }
}
