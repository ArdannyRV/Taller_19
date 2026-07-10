package com.epn.bloqueadorspam.utils

/**
 * Normaliza un número de teléfono para facilitar su comparación.
 * Elimina todos los caracteres que no sean dígitos y toma solo los últimos 9 dígitos.
 * (Ideal para comparar números locales ignorando prefijos de país como +593).
 */
fun normalizarNumeroTelefono(numero: String): String {
    val soloDigitos = numero.replace(Regex("[^0-9]"), "")
    return if (soloDigitos.length >= 9) {
        soloDigitos.takeLast(9)
    } else {
        soloDigitos
    }
}
