package com.parquimetro.feature.fiscal

import android.graphics.*
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

actual class FotoProcessor {
    actual fun processarComMarcaDagua(
        imagemBytes: ByteArray,
        nomeFiscal: String,
        timestamp: Long
    ): FotoProcessada {
        val original = BitmapFactory.decodeByteArray(imagemBytes, 0, imagemBytes.size)
        val bitmap = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)

        val data = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
            .format(Date(timestamp))

        val paint = Paint().apply {
            color = Color.argb(200, 255, 255, 255)
            textSize = bitmap.height * 0.035f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }

        val marcaDagua = "$nomeFiscal | $data"
        val x = 20f
        val y = bitmap.height - 30f
        canvas.drawText(marcaDagua, x, y, paint)

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        val bytes = out.toByteArray()

        val hash = MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

        return FotoProcessada(bytes = bytes, sha256 = hash)
    }
}
