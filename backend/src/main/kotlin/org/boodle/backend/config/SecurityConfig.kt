package org.boodle.backend.config

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.boodle.backend.model.UserDTO
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.crypto.SecretKey

@Configuration
class SecurityConfig {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun jwtTokenService(
        @Value("\${jwt.secret}") secret: String,
        @Value("\${jwt.issuer}") issuer: String,
        @Value("\${jwt.expiration-seconds}") expirationSeconds: Long
    ): JwtTokenService = JwtTokenService(secret, issuer, expirationSeconds)

    @Bean
    fun securityFilterChain(http: HttpSecurity, jwtAuthenticationFilter: JwtAuthenticationFilter): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/users").permitAll()
                auth.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                auth.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}

class JwtTokenService(
    private val secret: String,
    private val issuer: String,
    val expirationSeconds: Long
) {
    private val signingKey: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    fun generateToken(user: UserDTO): String {
        val now = Date()
        val expiration = Date(now.time + (expirationSeconds * 1000))

        return Jwts.builder()
            .subject(user.matr)
            .issuer(issuer)
            .claim("email", user.email)
            .claim("roles", listOf(user.rolle.name))
            .issuedAt(now)
            .expiration(expiration)
            .signWith(signingKey)
            .compact()
    }

    fun parseClaims(token: String): Claims =
        Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).payload
}

@Component
class JwtAuthenticationFilter(
    private val jwtTokenService: JwtTokenService
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = resolveToken(request)

        if (!token.isNullOrBlank() && SecurityContextHolder.getContext().authentication == null) {
            runCatching { jwtTokenService.parseClaims(token) }
                .onSuccess { claims ->
                    val roles = claims.getRoles()
                    val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }
                    val authentication = UsernamePasswordAuthenticationToken(claims.subject, token, authorities)
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authentication
                }
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val headerToken = request.getHeader("Authorization")
            ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?.substringAfter("Bearer ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        if (headerToken != null) {
            return headerToken
        }

        return request.getParameter("token")?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun Claims.getRoles(): List<String> = when (val value = this["roles"]) {
        is Collection<*> -> value.mapNotNull { element -> element?.toString()?.takeIf { it.isNotBlank() } }
        is Array<*> -> value.mapNotNull { element -> element?.toString()?.takeIf { it.isNotBlank() } }
        is String -> listOf(value).filter { it.isNotBlank() }
        else -> emptyList()
    }
}