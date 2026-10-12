package com.crossatrix.hub.data

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val email: String? = null,
    val created_at: String? = null,
)

@Serializable
data class LoginResponse(
    val user: UserDto,
    val cross_chat_id: String? = null,
    val crossi_ai_id: String? = null,
    val croins_balance: Double = 0.0,
    val is_student: Boolean = false,
    val is_teacher: Boolean = false,
    val is_principal: Boolean = false,
    val school_role: String? = null,
    val school_id: String? = null,
    val access_token: String,
    val refresh_token: String? = null,
    val expires_at: Long = 0L,
)

@Serializable
data class ErrorResponse(
    val error: String? = null,
    val code: String? = null,
)

sealed interface LoginResult {
    data class Success(val session: LoginResponse) : LoginResult
    data object Locked : LoginResult
    data class Failure(val message: String) : LoginResult
}
