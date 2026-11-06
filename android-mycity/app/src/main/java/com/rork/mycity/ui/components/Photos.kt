package com.rork.mycity.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** Saves a camera bitmap as a JPEG in app storage, stamped with GPS and time when known. */
suspend fun saveCameraPhoto(context: Context, bitmap: Bitmap, prefix: String, lat: Double? = null, lng: Double? = null): Uri? =
    withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "photos").apply { mkdirs() }
            val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            val exif = ExifInterface(file.absolutePath)
            exif.setAttribute(ExifInterface.TAG_DATETIME, SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date()))
            if (lat != null && lng != null) {
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, toDms(lat))
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (lat >= 0) "N" else "S")
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, toDms(lng))
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (lng >= 0) "E" else "W")
            }
            exif.saveAttributes()
            Uri.fromFile(file)
        }.getOrNull()
    }

/** Copies a picked gallery image into app storage so it survives after the picker grant ends. */
suspend fun copyPickedPhoto(context: Context, uri: Uri, prefix: String): Uri? = withContext(Dispatchers.IO) {
    runCatching {
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } } ?: return@runCatching null
        Uri.fromFile(file)
    }.getOrNull()
}

private fun toDms(value: Double): String {
    val v = abs(value)
    val d = v.toInt()
    val mFull = (v - d) * 60
    val m = mFull.toInt()
    val s = ((mFull - m) * 60 * 1000).toInt()
    return "$d/1,$m/1,$s/1000"
}
