package ru.kaplaan.authserver.service

import io.jsonwebtoken.*
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import ru.kaplaan.authserver.domain.entity.user.User
import java.security.Key
import java.util.*
import java.util.concurrent.TimeUnit

@Component
class JwtService {

    @Value("\${jwt.access.secret}")
    private lateinit var jwtAccessSecret: String

    @Value("\${jwt.refresh.secret}")
    private lateinit var jwtRefreshSecret: String

    @Value("\${jwt.refresh.expiration-minutes}")
    private var refreshTokenExpirationMinutes: Long = 0

    @Value("\${jwt.access.expiration-minutes}")
    private var accessTokenExpirationMinutes: Long = 0

    private val log = LoggerFactory.getLogger(javaClass)


    private fun getAccessSignKey(): Key =
        Decoders.BASE64.decode(jwtAccessSecret).let { bytes ->
            Keys.hmacShaKeyFor(bytes)
        }

    private fun getRefreshSignKey(): Key =
        Decoders.BASE64.decode(jwtRefreshSecret).let { bytes ->
            Keys.hmacShaKeyFor(bytes)
        }


    fun generateJwtAccessToken(user: User): String =
        Jwts
            .builder()
            .setSubject(user.username)
            .setClaims(
                mapOf(
                    "username" to user.username,
                    "userId" to user.id!!,
                    "password" to user.password
                )
            )
            .setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(accessTokenExpirationMinutes)))
            .signWith(getAccessSignKey(), SignatureAlgorithm.HS256)
            .compact()

    fun generateJwtRefreshToken(user: User): String =
        Jwts
            .builder()
            .setSubject(user.username)
            .setClaims(
                mapOf(
                    "username" to user.username,
                    "email" to user.email,
                    "userId" to user.id!!,
                    "password" to user.password
                )
            )
            .setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(refreshTokenExpirationMinutes)))
            .signWith(getRefreshSignKey(), SignatureAlgorithm.HS512)
            .compact()



    fun isValidAccessToken(accessToken: String): Boolean =
        validateToken(accessToken, getAccessSignKey()) && isNotExpired(accessToken, getAccessSignKey())


    fun isValidRefreshToken(refreshToken: String): Boolean =
        validateToken(refreshToken, getRefreshSignKey()) && isNotExpired(refreshToken, getRefreshSignKey())

    private fun validateToken(token: String, key: Key): Boolean =
        runCatching {
            Jwts
                .parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
        }
            .onFailure {

                when(it){
                    is ExpiredJwtException -> log.error("Token expired")

                    is UnsupportedJwtException -> log.error("Unsupported jwt")

                    is MalformedJwtException -> log.error("Malformed jwt")

                    is SignatureException -> log.error("Invalid signature")

                    else -> log.error("Invalid token")
                }
        }
            .isSuccess

    private fun isNotExpired(jwtToken: String, key: Key): Boolean =
        extractExpiration(jwtToken, key).after(Date(System.currentTimeMillis()))


    fun extractUsernameFromAccessToken(jwtToken: String): String =
        extractClaim(jwtToken, getAccessSignKey()){
            it["username"] as String
        }

    fun extractPasswordFromAccessToken(jwtToken: String): String =
        extractClaim(jwtToken, getAccessSignKey()) {
            it["password"] as String
        }


    private fun extractExpiration(jwtToken: String, key: Key): Date =
        extractClaim(jwtToken, key, Claims::getExpiration)


    private fun <T> extractClaim(jwtToken: String, key: Key, resolver: (Claims) -> T): T =
        resolver(extractAllClaims(jwtToken, key))


    private fun extractAllClaims(jwtToken: String, key: Key): Claims =
        Jwts
            .parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(jwtToken)
            .body
}