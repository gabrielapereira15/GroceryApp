package com.example.gpgrocery.data.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Product photos, kept in the app's private storage. A picture from the
 * camera or the gallery is scaled down and re-saved, so a 12-megapixel shot
 * of a milk carton does not cost 5 MB. ImageDecoder also turns it the right
 * way up.
 */
class PhotoStore(private val context: Context) {
    private val photosDir: File get() = File(context.filesDir, "photos").apply { mkdirs() }
    private val cameraDir: File get() = File(context.cacheDir, "camera").apply { mkdirs() }

    /** A place for the camera app to write a new picture into. */
    fun newCameraTarget(): Uri {
        val file = File(cameraDir, "shot-${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    /** Copies a picture into the store's photos and returns its path, or null if it cannot be read. */
    suspend fun keep(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > MAX_SIZE) {
                    val scale = MAX_SIZE.toFloat() / longest
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            val file = File(photosDir, "${UUID.randomUUID()}.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            bitmap.recycle()
            file.absolutePath
        }.getOrNull()
    }

    suspend fun remove(path: String?) = withContext(Dispatchers.IO) {
        path?.let { File(it).takeIf { file -> file.parentFile == photosDir }?.delete() }
        Unit
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        photosDir.listFiles()?.forEach(File::delete)
        cameraDir.listFiles()?.forEach(File::delete)
        Unit
    }

    private companion object {
        const val MAX_SIZE = 1280
    }
}
