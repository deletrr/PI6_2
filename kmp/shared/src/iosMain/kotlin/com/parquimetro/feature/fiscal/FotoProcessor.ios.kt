package com.parquimetro.feature.fiscal

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import platform.CommonCrypto.*

actual class FotoProcessor {
    actual fun processarComMarcaDagua(
        imagemBytes: ByteArray,
        nomeFiscal: String,
        timestamp: Long
    ): FotoProcessada {
        val nsData = imagemBytes.toNSData()
        val uiImage = UIImage.imageWithData(nsData) ?: error("Imagem inválida")

        UIGraphicsBeginImageContextWithOptions(uiImage.size, false, uiImage.scale)
        uiImage.drawAtPoint(CGPointMake(0.0, 0.0))

        val ctx = UIGraphicsGetCurrentContext()!!
        val data = NSDateFormatter().apply {
            dateFormat = "dd/MM/yyyy HH:mm"
            locale = NSLocale("pt_BR")
        }.stringFromDate(NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0))

        val texto = "$nomeFiscal | $data"
        val attrs = mapOf(
            NSFontAttributeName to UIFont.boldSystemFontOfSize(uiImage.size.useContents { height } * 0.035),
            NSForegroundColorAttributeName to UIColor.whiteColor
        )
        val nsString = texto as NSString
        nsString.drawAtPoint(
            CGPointMake(20.0, uiImage.size.useContents { height } - 40.0),
            withAttributes = attrs
        )

        val resultado = UIGraphicsGetImageFromCurrentImageContext()!!
        UIGraphicsEndImageContext()

        val jpegData = UIImageJPEGRepresentation(resultado, 0.9)!!
        val bytes = jpegData.toByteArray()
        val hash = sha256(bytes)

        return FotoProcessada(bytes = bytes, sha256 = hash)
    }

    private fun sha256(data: ByteArray): String {
        val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
        data.usePinned { pinned ->
            CC_SHA256(pinned.addressOf(0), data.size.toUInt(), digest.refTo(0))
        }
        return digest.joinToString("") { it.toString(16).padStart(2, '0') }
    }
}

private fun ByteArray.toNSData(): NSData =
    this.usePinned { NSData.dataWithBytes(it.addressOf(0), this.size.toULong()) }

private fun NSData.toByteArray(): ByteArray =
    ByteArray(this.length.toInt()).apply {
        this.usePinned { memcpy(it.addressOf(0), bytes, length) }
    }
