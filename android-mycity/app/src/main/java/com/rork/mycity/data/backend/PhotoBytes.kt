package com.rork.mycity.data.backend

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Turns a local photo into a JPEG ready to upload: at most [maxSide] pixels on the long
 * side, rotated upright, and re-encoded so camera metadata (including GPS) is stripped.
 * Returns null when there's no photo or it can't be read.
 */
suspend fun photoForUpload(context: Context, uri: String?, maxSide: Int = 1600): ByteArray? {
    if (uri.isNullOrBlank() || uri.startsWith("http")) return null
    return withContext(Dispatchers.IO) {
        runCatching {
            val u = Uri.parse(uri)
            val cr = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            val decoded = cr.openInputStream(u)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null
            val degrees = cr.openInputStream(u)?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
            val longSide = maxOf(decoded.width, decoded.height)
            val scale = if (longSide > maxSide) maxSide.toFloat() / longSide else 1f
            val m = Matrix().apply { if (scale < 1f) postScale(scale, scale); if (degrees != 0f) postRotate(degrees) }
            val out = if (scale < 1f || degrees != 0f) Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true) else decoded
            ByteArrayOutputStream().use { bos ->
                out.compress(Bitmap.CompressFormat.JPEG, 82, bos)
                bos.toByteArray()
            }
        }.getOrNull()
    }
}
