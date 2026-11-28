package com.fincip.nativeapp.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.google.gson.JsonObject
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Colour sets are all dark-on-light so a code stays scannable whatever the user picks. */
class QrPalette(val name: String, val a: Color, val b: Color, val bg: Color)
val QrPalettes = listOf(
    QrPalette("Violet", Color(0xFF5B3DF5), Color(0xFFB04DFF), Color(0xFFF6F2FF)),
    QrPalette("Ink", Color(0xFF14121F), Color(0xFF3A3558), Color(0xFFFFFFFF)),
    QrPalette("Forest", Color(0xFF14663F), Color(0xFF2E9E6B), Color(0xFFF1FAF4)),
    QrPalette("Sunset", Color(0xFFC2331F), Color(0xFFE5822B), Color(0xFFFFF6EA)),
    QrPalette("Ocean", Color(0xFF16408F), Color(0xFF1D8FD6), Color(0xFFEFF7FF)),
    QrPalette("Rose", Color(0xFF9B1C57), Color(0xFFE0457F), Color(0xFFFFF1F6)),
)
val QrDots = listOf("square", "round", "dot")

data class QrStyle(val palette: Int = 0, val dots: String = "round", val gradient: Boolean = true, val badge: Boolean = true) {
    fun toJson() = JsonObject().apply { addProperty("palette", palette); addProperty("dots", dots); addProperty("gradient", gradient); addProperty("badge", badge) }
    companion object {
        fun fromJson(o: JsonObject?) = if (o == null) QrStyle() else QrStyle(
            if (o.has("palette")) runCatching { o.get("palette").asInt }.getOrDefault(0).coerceIn(0, QrPalettes.lastIndex) else 0,
            if (o.has("dots")) o.get("dots").asString.takeIf { it in QrDots } ?: "round" else "round",
            !o.has("gradient") || runCatching { o.get("gradient").asBoolean }.getOrDefault(true),
            !o.has("badge") || runCatching { o.get("badge").asBoolean }.getOrDefault(true)
        )
    }
}

private fun matrixOf(text: String): Array<BooleanArray> {
    val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H, EncodeHintType.MARGIN to 0))
    return Array(m.height) { y -> BooleanArray(m.width) { x -> m.get(x, y) } }
}

fun DrawScope.drawQr(grid: Array<BooleanArray>, style: QrStyle, initial: String) {
    val pal = QrPalettes[style.palette]
    val quiet = size.minDimension * .06f
    drawRoundRect(pal.bg, Offset.Zero, size, CornerRadius(size.minDimension * .08f))
    val n = grid.size; val side = size.minDimension - quiet * 2; val cell = side / n
    val brush = if (style.gradient) Brush.linearGradient(listOf(pal.a, pal.b), Offset(quiet, quiet), Offset(quiet + side, quiet + side)) else Brush.linearGradient(listOf(pal.a, pal.a))
    val hole = if (style.badge) (n * .2f).toInt().coerceAtLeast(5) else 0
    val lo = (n - hole) / 2; val hi = lo + hole
    fun finder(x: Int, y: Int) = (x < 7 && y < 7) || (x >= n - 7 && y < 7) || (x < 7 && y >= n - 7)
    for (y in 0 until n) for (x in 0 until n) {
        if (!grid[y][x]) continue
        if (hole > 0 && x in lo until hi && y in lo until hi) continue
        val tl = Offset(quiet + x * cell, quiet + y * cell)
        if (finder(x, y) || style.dots == "square") drawRect(brush, tl, Size(cell + .6f, cell + .6f))
        else if (style.dots == "round") drawRoundRect(brush, tl, Size(cell, cell), CornerRadius(cell * .38f))
        else drawCircle(brush, cell * .46f, tl + Offset(cell / 2, cell / 2))
    }
    if (hole > 0) {
        val c = Offset(size.width / 2, size.height / 2); val r = hole * cell * .55f
        drawCircle(pal.bg, r * 1.12f, c); drawCircle(brush, r, c)
        drawCircle(Color.White.copy(alpha = .9f), r * .78f, c)
        val p = androidx.compose.ui.graphics.Path().apply { moveTo(c.x - r * .35f, c.y + r * .35f); cubicTo(c.x - r * .5f, c.y - r * .3f, c.x, c.y - r * .55f, c.x + r * .42f, c.y - r * .42f); cubicTo(c.x + r * .45f, c.y, c.x + r * .1f, c.y + r * .45f, c.x - r * .35f, c.y + r * .35f); close() }
        drawPath(p, brush)
    }
}

@Composable
fun QrView(payload: String, style: QrStyle, size: Dp, modifier: Modifier = Modifier) {
    val grid = remember(payload) { matrixOf(payload) }
    Canvas(modifier.size(size)) { drawQr(grid, style, "") }
}

fun renderQrBitmap(payload: String, style: QrStyle, px: Int = 1024): Bitmap {
    val img = androidx.compose.ui.graphics.ImageBitmap(px, px)
    val grid = matrixOf(payload)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(img), Size(px.toFloat(), px.toFloat())) { drawQr(grid, style, "") }
    return img.asAndroidBitmap()
}

/** Saves to Pictures/FinCip. Needs Android 10+ because older versions require a storage permission the app doesn't ask for. */
fun saveQrToGallery(context: Context, payload: String, style: QrStyle): String {
    if (Build.VERSION.SDK_INT < 29) return "Saving to the gallery needs Android 10 or newer."
    return runCatching {
        val bmp = renderQrBitmap(payload, style)
        val v = ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME, "FinCip-QR-${System.currentTimeMillis()}.png"); put(MediaStore.Images.Media.MIME_TYPE, "image/png"); put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FinCip") }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v) ?: error("no uri")
        context.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        "Saved to Pictures/FinCip"
    }.getOrElse { "Couldn't save: ${it.message}" }
}
