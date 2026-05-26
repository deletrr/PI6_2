#pragma once
#include <Arduino.h>
#include <mbedtls/md.h>

String generateHmacSha256(const String& payload, const String& secretKey) {
    byte hmac[32];
    mbedtls_md_context_t ctx;
    mbedtls_md_init(&ctx);
    mbedtls_md_setup(&ctx, mbedtls_md_info_from_type(MBEDTLS_MD_SHA256), 1);
    mbedtls_md_hmac_starts(&ctx,
        (const unsigned char*)secretKey.c_str(), secretKey.length());
    mbedtls_md_hmac_update(&ctx,
        (const unsigned char*)payload.c_str(), payload.length());
    mbedtls_md_hmac_finish(&ctx, hmac);
    mbedtls_md_free(&ctx);

    String result;
    result.reserve(64);
    for (int i = 0; i < 32; i++) {
        if (hmac[i] < 0x10) result += '0';
        result += String(hmac[i], HEX);
    }
    return result;
}
