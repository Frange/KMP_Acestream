package com.frange.kmpacestream.shared.domain.core

sealed interface NetworkError {
    data object RequestTimeout : NetworkError
    data object Unauthorized : NetworkError
    data object NoInternet : NetworkError
    data object ServerError : NetworkError
    data object Serialization : NetworkError
    data object Refused : NetworkError
    data object Unknown : NetworkError
}

fun NetworkError.toUiText(): String {
    return when (this) {
        NetworkError.RequestTimeout -> "La solicitud ha expirado. Inténtalo de nuevo."
        NetworkError.Unauthorized -> "No tienes autorización para realizar esta acción."
        NetworkError.NoInternet -> "Sin conexión a internet. Comprueba tu red."
        NetworkError.ServerError -> "Error en el servidor. Inténtalo más tarde."
        NetworkError.Serialization -> "Error al procesar los datos recibidos."
        NetworkError.Unknown -> "Ha ocurrido un error desconocido."
        NetworkError.Refused -> "Error, enlace rechazado"
    }
}