package com.fincip.nativeapp.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.File

/** 24 ready-made characters the user can pick as their avatar. */
val AvatarPresets: List<PetLook> = (0 until 24).map { i ->
    val sp = i % 9
    PetLook(
        species = sp, skin = if (sp >= 2) i % 10 else i % 6, hairStyle = i % 8, hairColor = (i * 3) % 10,
        outfitStyle = i % 5, outfitColor = (i * 2) % 9, accessory = listOf(0, 0, 1, 0, 3, 0, 8, 0, 9, 0, 15, 0)[i % 12],
        background = i % 8, eyes = i % 4, eyeColor = i % 8, freckles = i % 5 == 0, mouth = i % 5, brows = i % 3, blush = i % 2 == 0
    )
}

fun userImageFile(context: Context, banner: Boolean) = File(context.filesDir, if (banner) "banner.jpg" else "avatar.jpg")

/** Reads the picked image, downsizes it and stores it privately inside the app. Returns true on success. */
fun saveUserImage(context: Context, uri: Uri, banner: Boolean): Boolean = runCatching {
    val maxSide = if (banner) 1280 else 512
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide * 2) sample *= 2
    val raw = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return false
    val scale = maxSide.toFloat() / maxOf(raw.width, raw.height)
    val bmp = if (scale < 1f) Bitmap.createScaledBitmap(raw, (raw.width * scale).toInt().coerceAtLeast(1), (raw.height * scale).toInt().coerceAtLeast(1), true) else raw
    userImageFile(context, banner).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
    true
}.getOrDefault(false)

@Composable
fun rememberUserImage(context: Context, banner: Boolean, stamp: Long): ImageBitmap? =
    remember(stamp, banner) { runCatching { BitmapFactory.decodeFile(userImageFile(context, banner).path)?.asImageBitmap() }.getOrNull() }

/** The user's avatar: their own photo, a preset character, or the generated face. */
@Composable
fun UserAvatar(profile: ProfileData, size: Dp = 64.dp, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    when (profile.avatarMode) {
        "photo" -> {
            val img = rememberUserImage(context, false, profile.photoStamp)
            if (img != null) Image(img, null, modifier.size(size).clip(CircleShape), contentScale = ContentScale.Crop)
            else ProfileAvatar(profile.seed, modifier, size)
        }
        "preset" -> Box(modifier.size(size).clip(CircleShape)) {
            PetCharacter(AvatarPresets[profile.preset.coerceIn(0, AvatarPresets.lastIndex)], PetMood.HAPPY, 6, diameter = size)
        }
        else -> ProfileAvatar(profile.seed, modifier, size)
    }
}
