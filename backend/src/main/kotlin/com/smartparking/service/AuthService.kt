package com.smartparking.service

import com.smartparking.dto.*
import com.smartparking.entity.User
import com.smartparking.entity.UserRole
import com.smartparking.repository.UserRepository
import com.smartparking.security.CustomUserDetailsService
import com.smartparking.security.JwtService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val authenticationManager: AuthenticationManager,
    private val userDetailsService: CustomUserDetailsService
) {

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        val cleanCpf = request.cpf.replace(Regex("[^0-9]"), "")
        val formattedCpf = formatCpf(cleanCpf)

        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("E-mail já cadastrado.")
        }
        if (userRepository.existsByCpf(formattedCpf)) {
            throw IllegalArgumentException("CPF já cadastrado.")
        }

        val user = User(
            name = request.name.trim(),
            email = request.email.lowercase().trim(),
            passwordHash = passwordEncoder.encode(request.password),
            cpf = formattedCpf,
            phone = request.phone?.trim(),
            role = UserRole.USER
        )

        userRepository.save(user)

        val userDetails = userDetailsService.loadUserByUsername(user.email)
        val token = jwtService.generateToken(userDetails)
        return AuthResponse(token = token, user = user.toResponse())
    }

    fun login(request: LoginRequest): AuthResponse {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email.lowercase().trim(), request.password)
        )
        val user = userDetailsService.loadUserEntityByEmail(request.email.lowercase().trim())
        val userDetails = userDetailsService.loadUserByUsername(user.email)
        val token = jwtService.generateToken(userDetails)
        return AuthResponse(token = token, user = user.toResponse())
    }

    private fun formatCpf(digits: String): String {
        if (digits.length != 11) return digits
        return "${digits.substring(0, 3)}.${digits.substring(3, 6)}.${digits.substring(6, 9)}-${digits.substring(9)}"
    }
}
