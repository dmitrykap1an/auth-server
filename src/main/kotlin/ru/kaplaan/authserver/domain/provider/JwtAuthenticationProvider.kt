package ru.kaplaan.authserver.domain.provider

import org.springframework.security.authentication.*
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService

class JwtAuthenticationProvider(
    private val userDetailsService: UserDetailsService
): AbstractUserDetailsAuthenticationProvider() {
    override fun authenticate(authentication: Authentication): Authentication {
        val userDetails = retrieveUser(authentication.name, authentication as UsernamePasswordAuthenticationToken)
        additionalAuthenticationChecks(userDetails, authentication)
        return UsernamePasswordAuthenticationToken.authenticated(
            userDetails,
            authentication.credentials as String,
            userDetails.authorities
        )
    }

    override fun additionalAuthenticationChecks(
        userDetails: UserDetails,
        authentication: UsernamePasswordAuthenticationToken,
    ) {
        when{
            userDetails.password != authentication.credentials as String -> throw BadCredentialsException("Неверный пароль!")

            userDetails.username != authentication.name -> throw BadCredentialsException("Неверный username!")

            !userDetails.isEnabled -> throw AuthenticationServiceException("Аккаунт не доступен!")

            !userDetails.isAccountNonLocked -> throw LockedException("Аккаунт заблокирован!")

            !userDetails.isCredentialsNonExpired -> throw CredentialsExpiredException("Срок действия данных пользователя истек!")
        }
    }

    override fun retrieveUser(username: String, authentication: UsernamePasswordAuthenticationToken): UserDetails {
        return userDetailsService.loadUserByUsername(username)
    }


}