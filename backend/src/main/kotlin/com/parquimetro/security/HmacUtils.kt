package com.parquimetro.security

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacUtils {
    fun compute(payload: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun verify(payload: String, secret: String, expected: String): Boolean =
        compute(payload, secret).equals(expected, ignoreCase = true)
}
